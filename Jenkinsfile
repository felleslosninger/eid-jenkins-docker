import java.time.ZoneId
import java.time.format.DateTimeFormatter

import static java.time.ZonedDateTime.now

pipeline {
    agent none
    options {
        timeout(time: 5, unit: 'DAYS')
        disableConcurrentBuilds()
    }
    stages {
        stage('Build') {
            when { expression { env.BRANCH_NAME.matches(/(feature|bugfix)\/(\w+-\w+)/) } }
            agent any
            steps {
                script {
                    env.version = DateTimeFormatter.ofPattern('yyyy-MM-dd-HHmm').format(now(ZoneId.of('UTC')))
                    env.commitId = readCommitId().take(7)
                    currentBuild.description = "Building: ${env.commitId}"
                    sh "./build.sh verify"
                }
            }
        }
        stage('Prepare verification') {
            when { expression { env.BRANCH_NAME.matches(/(feature|bugfix)\/(\w+-\w+)/) } }
            environment {
                crucible = credentials('crucible')
            }
            agent any
            steps {
                script {
                    if (readCommitMessage() != "ready!") error("Developer has not signalled that work is ready for verification")
                    sshagent(['ssh.github.com']) {
                        env.verifyRevision = sh(returnStdout: true, script: "pipeline/create-verification-revision")
                    }
                    sh "pipeline/create-review ${env.verifyRevision} ${env.crucible_USR} ${env.crucible_PSW}"
                }
            }
            post { failure { sshagent(['ssh.github.com']) { sh "pipeline/delete-verification-revision" }}}
        }
        stage('Approve code') {
            when { expression { env.BRANCH_NAME.matches(/(feature|bugfix)\/(\w+-\w+)/) } }
            steps {
                script {
                    try {
                        input message: "Has the code been reviewed and approved?", ok: "Yes"
                        env.codeApproved = "true"
                    } catch (Exception ignored) {
                        env.codeApproved = "false"
                    }
                }
            }
        }
        stage('Deliver') {
            when { expression { env.BRANCH_NAME.matches(/(feature|bugfix)\/(\w+-\w+)/) } }
            environment {
                nexus = credentials('nexus')
            }
            agent any
            steps {
                script {
                    ansiColor('xterm') { sh "./build.sh deliver ${env.version} ${env.nexus_USR} ${env.nexus_PSW}" }
                }
            }
            post { failure { sshagent(['ssh.github.com']) { sh "pipeline/delete-verification-revision" }}}
        }
        stage('Integrate') {
            when { expression { env.BRANCH_NAME.matches(/(feature|bugfix)\/(\w+-\w+)/) } }
            agent any
            steps {
                script {
                    if (env.codeApproved.equals("false")) error("Code not approved")
                    sshagent(['ssh.github.com']) {
                        ansiColor('xterm') { sh 'pipeline/integrate-branch' }
                    }
                }
            }
            post { failure { sshagent(['ssh.github.com']) { sh "pipeline/delete-verification-revision" }}}
        }
        stage('Deploy') {
            when { expression { env.BRANCH_NAME.matches(/(feature|bugfix)\/(\w+-\w+)/) } }
            agent any
            steps {
                script {
                    sshagent(['ssh.git.difi.local']) {
                        sh "scp application-stack.yml jenkins@eid-jenkins02.dmz.local:"
                        sh 'ssh jenkins@eid-jenkins02.dmz.local "eval \\$(keychain --eval id_all_jenkins); VERSION=' + env.version + ' uid=\\$(id -u jenkins) gid=\\$(id -g jenkins) docker stack deploy -c application-stack.yml pipeline"'
                        sh "ssh jenkins@eid-jenkins02.dmz.local rm application-stack.yml"
                    }
                }
            }
        }
    }
    post {
        changed {
            notifySuccess()
        }
        unstable {
            echo "Unstable"
            notifyUnstable()
        }
        failure {
            echo "Failure"
            notifyFailed()
        }
        aborted {
            notifyFailed()
            echo "Aborted"
        }
        always {
            echo "Finish building ${env.commitMessage}"
        }
    }
}

def notifyFailed() {
    emailext (
            subject: "FAILED: '${env.JOB_NAME}'",
            body: """<p>FAILED: Bygg '${env.JOB_NAME} [${env.BUILD_NUMBER}]' feilet.</p>
            <p><b>Konsoll output:</b><br/>
            <a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>""",
            recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']]
    )
}

def notifyUnstable() {
    emailext (
            subject: "UNSTABLE: '${env.JOB_NAME}'",
            body: """<p>UNSTABLE: Bygg '${env.JOB_NAME} [${env.BUILD_NUMBER}]' er ustabilt.</p>
            <p><b>Konsoll output:</b><br/>
            <a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a></p>""",
            recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']]
    )
}

def notifySuccess() {
    if (isPreviousBuildFailOrUnstable()) {
        emailext (
                subject: "SUCCESS: '${env.JOB_NAME}'",
                body: """<p>SUCCESS: Bygg '${env.JOB_NAME} [${env.BUILD_NUMBER}]' er oppe og snurrer igjen.</p>""",
                recipientProviders: [[$class: 'CulpritsRecipientProvider'], [$class: 'RequesterRecipientProvider']]
        )
    }
}

boolean isPreviousBuildFailOrUnstable() {
    if(!hudson.model.Result.SUCCESS.equals(currentBuild.rawBuild.getPreviousBuild()?.getResult())) {
        return true
    }
    return false
}

String readCommitId() {
    return sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
}

String readCommitMessage() {
    return sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
}
