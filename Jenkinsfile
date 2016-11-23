#!groovy
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import static java.time.ZonedDateTime.now

String version = DateTimeFormatter.ofPattern('yyyyMMddHHmm').format(now(ZoneId.of('UTC')))

stage('Build') {

    node {
        checkout scm
        env.commitId = readCommitId()
        env.commitMessage = readCommitMessage()
        currentBuild.description = "Commit: ${env.commitId}"
        if (isDeployBuild()) {
            currentBuild.displayName = "#${currentBuild.number}: Deploy build for version ${version}"
            sh "pipeline/build.sh deliver ${version}"
            sh "ssh 'eid-jenkins02.dmz.local' bash -s -- < pipeline/application.sh update ${version}"
        } else if (isQaBuild()) {
            currentBuild.displayName = "#${currentBuild.number}: QA build for version ${version}"
            sh "pipeline/build.sh verify"
        } else if (isQuickBuild()) {
            currentBuild.displayName = "#${currentBuild.number}: Quick build"
            sh "pipeline/build.sh verify"
        }
    }

}

boolean isDeployBuild() {
    return env.BRANCH_NAME.matches('master')
}

boolean isQaBuild() {
    return env.commitMessage.startsWith("qa!") || env.BRANCH_NAME.matches(/feature\/qa\/(\w+-\w+)/)
}

boolean isQuickBuild() {
    return env.BRANCH_NAME.matches(/(feature|bugfix)\/(\w+-\w+)/)
}

String readCommitId() {
    return sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
}

String readCommitMessage() {
    return sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
}
