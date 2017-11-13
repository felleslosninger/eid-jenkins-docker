import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException

import static java.time.ZonedDateTime.now

def buildHostUser = 'jenkins'
def buildHostName = 'eid-jenkins03.dmz.local'
def buildHostSshKey = 'ssh.git.difi.local'
def deployStackName = 'pipeline'
def deployHostName = 'eid-jenkins02.dmz.local'
def deployHostUser = 'jenkins'
def deployHostSshKey = 'ssh.git.difi.local'
def gitSshKey = 'ssh.github.com'

pipeline {
    agent none
    options {
        timeout(time: 5, unit: 'DAYS')
        disableConcurrentBuilds()
        ansiColor('xterm')
        timestamps()
    }
    stages {
        stage('Check build') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) } }
            agent any
            steps {
                transitionIssue env.ISSUE_STATUS_OPEN, env.ISSUE_TRANSITION_START
                ensureIssueStatusIs env.ISSUE_STATUS_IN_PROGRESS
                script {
                    currentBuild.description = "Building from commit " + readCommitId()
                    if (readCommitMessage() == "ready!") {
                        env.verification = 'true'
                    }
                }
                sh "docker/build verify"
            }
        }
        stage('Wait for verification to start') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            steps {
                transitionIssue env.ISSUE_TRANSITION_READY_FOR_CODE_REVIEW
                waitUntilIssueStatusIs env.ISSUE_STATUS_CODE_REVIEW
            }
        }
        stage('Wait for verification slot') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            agent any
            steps {
                failIfJobIsAborted()
                sshagent([gitSshKey]) {
                    retry(count: 1000000) {
                        sleep 10
                        sh 'pipeline/git/available-verification-slot'
                    }
                }
            }
            post {
                failure { transitionIssue env.ISSUE_STATUS_CODE_REVIEW, env.ISSUE_TRANSITION_RESUME_WORK }
                aborted { transitionIssue env.ISSUE_STATUS_CODE_REVIEW, env.ISSUE_TRANSITION_RESUME_WORK }
            }
        }
        stage('Prepare verification') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            environment {
                crucible = credentials('crucible')
            }
            agent any
            steps {
                script {
                    env.version = DateTimeFormatter.ofPattern('yyyy-MM-dd-HHmm').format(now(ZoneId.of('UTC'))) + "-" + readCommitId()
                    commitMessage = "${env.version}|" + issueId() + ": " + issueSummary()
                    sshagent([gitSshKey]) {
                        verifyRevision = sh returnStdout: true, script: "pipeline/git/create-verification-revision \"${commitMessage}\""
                    }
                    sh "pipeline/create-review ${verifyRevision} ${env.crucible_USR} ${env.crucible_PSW}"
                }
            }
            post {
                failure {
                    deleteVerificationBranch(gitSshKey)
                    transitionIssue env.ISSUE_STATUS_CODE_REVIEW, env.ISSUE_TRANSITION_RESUME_WORK
                }
                aborted {
                    deleteVerificationBranch(gitSshKey)
                    transitionIssue env.ISSUE_STATUS_CODE_REVIEW, env.ISSUE_TRANSITION_RESUME_WORK
                }
            }
        }
        stage('Deliver artifacts') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
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
                    checkoutVerificationBranch()
                    DOCKER_HOST = sh(returnStdout: true, script: 'pipeline/docker/define-docker-host-for-ssh-tunnel')
                    sshagent([buildHostSshKey]) {
                        sh "DOCKER_HOST=${DOCKER_HOST} pipeline/docker/create-ssh-tunnel-for-docker-host ${buildHostUser}@${buildHostName}"
                    }
                    sh "DOCKER_TLS_VERIFY= DOCKER_HOST=${DOCKER_HOST} docker/build deliver ${env.version} ${env.nexus_USR} ${env.nexus_PSW}"
                }
            }
            post {
                failure {
                    deleteVerificationBranch(gitSshKey)
                    transitionIssue env.ISSUE_STATUS_CODE_REVIEW, env.ISSUE_TRANSITION_RESUME_WORK
                }
                aborted {
                    deleteVerificationBranch(gitSshKey)
                    transitionIssue env.ISSUE_STATUS_CODE_REVIEW, env.ISSUE_TRANSITION_RESUME_WORK
                }
            }
        }
        stage('Deploy for verification') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            environment {
                nexus = credentials('nexus')
            }
            agent any
            steps {
                echo "Verification not implemented"
            }
        }
        stage('Verify behaviour') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            agent any
            steps {
                echo "Verification not implemented"
            }
            post {
                failure {
                    deleteVerificationBranch(gitSshKey)
                    transitionIssue env.ISSUE_STATUS_CODE_REVIEW, env.ISSUE_TRANSITION_RESUME_WORK
                }
                aborted {
                    deleteVerificationBranch(gitSshKey)
                    transitionIssue env.ISSUE_STATUS_CODE_REVIEW, env.ISSUE_TRANSITION_RESUME_WORK
                }
            }
        }
        stage('Wait for code review to finish') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            steps {
                waitUntilIssueStatusIsNot env.ISSUE_STATUS_CODE_REVIEW
                script {
                    env.codeApproved = "false"
                    if (issueStatusIs(env.ISSUE_STATUS_CODE_APPROVED))
                        env.codeApproved = "true"
                }
            }
        }
        stage('Integrate code') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            agent {
                dockerfile {
                    dir 'docker'
                    args '-v /var/jenkins_home/.ssh/known_hosts:/root/.ssh/known_hosts -u root:root'
                }
            }
            steps {
                failIfJobIsAborted()
                failIfCodeNotApproved()
                checkoutVerificationBranch()
                integrateCode(gitSshKey)
            }
            post {
                always {
                    deleteVerificationBranch(gitSshKey)
                }
                success {
                    deleteWorkBranch(gitSshKey)
                }
                failure {
                    transitionIssue env.ISSUE_STATUS_CODE_REVIEW, env.ISSUE_TRANSITION_RESUME_WORK
                }
                aborted {
                    transitionIssue env.ISSUE_STATUS_CODE_REVIEW, env.ISSUE_TRANSITION_RESUME_WORK
                }
            }
        }
        stage('Wait for manual verification to start') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            steps {
                waitUntilIssueStatusIs env.ISSUE_STATUS_MANUAL_VERIFICATION
            }
        }
        stage('Deploy for manual verification') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            agent any
            steps {
                failIfJobIsAborted()
                echo "Deploy for manual verification not implemented"
            }
        }
        stage('Wait for manual verification to finish') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            steps {
                waitUntilIssueStatusIsNot env.ISSUE_STATUS_MANUAL_VERIFICATION
                failIfJobIsAborted()
                ensureIssueStatusIs env.ISSUE_STATUS_MANUAL_VERIFICATION_OK
            }
        }
        stage('Deploy for production') {
            when { expression { env.BRANCH_NAME.matches(/(work|feature|bugfix)\/(\w+-\w+)/) && env.verification == 'true' } }
            environment {
                nexus = credentials('nexus')
            }
            agent any
            steps {
                script {
                    sshagent([deployHostSshKey]) {
                        sh "ssh ${deployHostUser}@${deployHostName} mkdir -p /tmp/${env.BRANCH_NAME}"
                        sh "scp docker/stack.yml docker/run ${deployHostUser}@${deployHostName}:/tmp/${env.BRANCH_NAME}"
                        sh "ssh ${deployHostUser}@${deployHostName} /tmp/${env.BRANCH_NAME}/run ${env.nexus_USR} ${env.nexus_PSW} ${deployStackName} ${env.version}"
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

def integrateCode(def sshKey) {
    sshagent([sshKey]) { sh 'git push origin HEAD:master' }
}

def checkoutVerificationBranch() {
    sh "git checkout verify/\${BRANCH_NAME}"
    sh "git reset --hard origin/verify/\${BRANCH_NAME}"
}

def deleteVerificationBranch(def sshKey) {
    echo "Deleting verification branch"
    sshagent([sshKey]) { sh "git push origin --delete verify/\${BRANCH_NAME}" }
    echo "Verification branch deleted"
}

def deleteWorkBranch(def sshKey) {
    sshagent([sshKey]) { sh "git push origin --delete \${BRANCH_NAME}" }
}

def failIfJobIsAborted() {
    if (env.jobAborted == 'true')
        error('Job was aborted')
}

def failIfCodeNotApproved() {
    if (env.codeApproved == 'false')
        error('Code was not approved')
}

boolean issueStatusIs(def targetStatus) {
    return issueStatus() == targetStatus
}

def waitUntilIssueStatusIs(def targetStatus) {
    env.jobAborted = 'false'
    try {
        retry(count: 1000000) {
            if (!issueStatusIs(targetStatus)) {
                sleep 10
                error "Waiting until issue status is ${targetStatus}..."
            }
        }
    } catch (FlowInterruptedException ignored) {
        env.jobAborted = "true"
    }
}

def waitUntilIssueStatusIsNot(def targetStatus) {
    env.jobAborted = 'false'
    try {
        retry(count: 1000000) {
            if (issueStatusIs(targetStatus)) {
                sleep 10
                error "Waiting until issue status is not ${targetStatus}..."
            }
        }
    } catch (FlowInterruptedException ignored) {
        env.jobAborted = "true"
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

def issueId() {
    return env.BRANCH_NAME.tokenize('/')[-1]
}

def transitionIssue(def transitionId) {
    jiraTransitionIssue idOrKey: issueId(), input: [transition: [id: transitionId]]
}

def transitionIssue(def sourceStatus, def transitionId) {
    if (issueStatusIs(sourceStatus))
        transitionIssue transitionId
}

def ensureIssueStatusIs(def issueStatus) {
    if (!issueStatusIs(issueStatus))
        error "Issue status is not ${issueStatus}"
}

String issueStatus() {
    return jiraGetIssue(idOrKey: issueId()).data.fields['status']['id']
}

String issueSummary() {
    return jiraGetIssue(idOrKey: issueId()).data.fields['summary']
}

def readCommitId() {
    return sh(returnStdout: true, script: 'git rev-parse HEAD').trim().take(7)
}

def readCommitMessage() {
    return sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
}
