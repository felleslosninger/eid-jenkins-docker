import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

import static java.time.ZonedDateTime.now

def buildHostUser = 'jenkins'
def buildHostName = 'eid-jenkins03.dmz.local'

pipeline {
    agent none
    options {
        timeout(time: 5, unit: 'DAYS')
        disableConcurrentBuilds()
        ansiColor('xterm')
    }
    stages {
        stage('Check build') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) } }
            agent any
            steps {
                script {
                    currentBuild.description = "Building from commit " + readCommitId()
                    if (readCommitMessage() == "ready!") {
                        env.verification = 'true'
                    }
                }
                sh "docker/build verify"
            }
        }
        stage('Wait for code reviewer to start') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            steps {
                script {
                    retry(count: 1000000) {
                        if (issueStatus(issueId(env.BRANCH_NAME)) != env.ISSUE_STATUS_CODE_REVIEW) {
                            sleep 10
                            error("Issue is not yet under code review")
                        }
                    }
                }
            }
        }
        stage('Wait for verification slot') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            agent any
            steps {
                script {
                    sshagent(['ssh.github.com']) {
                        retry(count: 1000000) {
                            sleep 10
                            sh 'pipeline/git/available-verification-slot'
                        }
                    }
                }
            }
        }
        stage('Create code review') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            environment {
                crucible = credentials('crucible')
            }
            agent any
            steps {
                script {
                    version = DateTimeFormatter.ofPattern('yyyy-MM-dd-HHmm').format(now(ZoneId.of('UTC'))) + "-" + readCommitId()
                    sshagent(['ssh.github.com']) {
                        verifyRevision = sh returnStdout: true, script: "pipeline/git/create-verification-revision ${version}"
                    }
                    sh "pipeline/create-review ${verifyRevision} ${env.crucible_USR} ${env.crucible_PSW}"
                }
            }
            post {
                failure { sshagent(['ssh.github.com']) { sh "git push origin --delete verify/\${BRANCH_NAME}" }}
                aborted { sshagent(['ssh.github.com']) { sh "git push origin --delete verify/\${BRANCH_NAME}" }}
            }
        }
        stage('Wait for code reviewer to finish') {
            when { expression { env.BRANCH_NAME.matches(/verify\/(work|feature|bugfix)\/(\w+-\w+)/) } }
            steps {
                script {
                    env.codeApproved = "false"
                    env.jobAborted = "false"
                    try {
                        retry(count: 1000000) {
                            if (issueStatus(issueId(env.BRANCH_NAME)) == env.ISSUE_STATUS_CODE_REVIEW) {
                                sleep 10
                                error("Issue is still under code review")
                            }
                        }
                        if (issueStatus(issueId(env.BRANCH_NAME)) == env.ISSUE_STATUS_CODE_APPROVED)
                            env.codeApproved = "true"
                    } catch (FlowInterruptedException e) {
                        env.jobAborted = "true"
                    }
                }
            }
        }
        stage('Deliver') {
            when { expression { env.BRANCH_NAME.matches(/verify\/(work|feature|bugfix)\/(\w+-\w+)/) } }
            environment {
                nexus = credentials('nexus')
            }
            agent {
                dockerfile {
                    dir 'docker'
                    args '-v /var/jenkins_home/.ssh/known_hosts:/root/.ssh/known_hosts -u root:root'
                }
            }
            steps {
                script {
                    if (env.jobAborted == "true") {
                        error("Job was aborted")
                    } else if (env.codeApproved == "false") {
                        error("Code was not approved")
                    }
                    version = versionFromCommitMessage()
                    DOCKER_HOST = sh(returnStdout: true, script: 'pipeline/docker/define-docker-host-for-ssh-tunnel')
                    sshagent(['ssh.git.difi.local']) {
                        sh "DOCKER_HOST=${DOCKER_HOST} pipeline/docker/create-ssh-tunnel-for-docker-host ${buildHostUser}@${buildHostName}"
                    }
                    sh "DOCKER_TLS_VERIFY= DOCKER_HOST=${DOCKER_HOST} docker/build deliver ${version} ${env.nexus_USR} ${env.nexus_PSW}"
                }
            }
            post {
                failure { sshagent(['ssh.github.com']) { sh "git push origin --delete \${BRANCH_NAME}" }}
                aborted { sshagent(['ssh.github.com']) { sh "git push origin --delete \${BRANCH_NAME}" }}
            }
        }
        stage('Integrate code') {
            when { expression { env.BRANCH_NAME.matches(/verify\/(work|feature|bugfix)\/(\w+-\w+)/) } }
            agent any
            steps {
                script {
                    sshagent(['ssh.github.com']) {
                        sh 'git push origin HEAD:master'
                    }
                }
            }
            post {
                always {
                    sshagent(['ssh.github.com']) { sh "git push origin --delete \${BRANCH_NAME}" }
                }
                success {
                    sshagent(['ssh.github.com']) { sh "git push origin --delete \${BRANCH_NAME#verify/}" }
                }
            }
        }
        stage('Deploy') {
            when { expression { env.BRANCH_NAME.matches(/verify\/(work|feature|bugfix)\/(\w+-\w+)/) } }
            agent any
            steps {
                script {
                    version = versionFromCommitMessage()
                    sshagent(['ssh.git.difi.local']) {
                        sh "ssh jenkins@eid-jenkins02.dmz.local mkdir -p /tmp/${env.BRANCH_NAME}"
                        sh "scp docker/stack.yml docker/run jenkins@eid-jenkins02.dmz.local:/tmp/${env.BRANCH_NAME}"
                        sh "ssh jenkins@eid-jenkins02.dmz.local /tmp/${env.BRANCH_NAME}/run ${version}"
                    }
                }
            }
        }
    }
    post {
        success {
            echo "Success"
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
            echo "Aborted"
            notifyFailed()
        }
        always {
            echo "Build finished"
        }
    }
}

String versionFromCommitMessage() {
    return readCommitMessage().tokenize(':')[0]
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

static def issueId(def branchName) {
    return branchName.tokenize('/')[-1]
}

String issueStatus(def issueId) {
    return jiraGetIssue(idOrKey: issueId, site: 'jira').data.fields['status']['id']
}

def readCommitId() {
    return sh(returnStdout: true, script: 'git rev-parse HEAD').trim().take(7)
}

def readCommitMessage() {
    return sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
}
