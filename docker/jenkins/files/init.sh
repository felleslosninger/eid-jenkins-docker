#!/usr/bin/env bash

createConfiguration() {
    local issueStatusCodeApproved=${1}
    local issueStatusCodeReview=${2}
    local issueStatusManualVerification=${3}
    local issueStatusManualVerificationOk=${4}
    local dockerHost=${5}
    local file=${JENKINS_HOME}/config.xml
    cp /files/template-config.xml ${file}
    sed -i "s|\${ISSUE_STATUS_CODE_APPROVED}|${issueStatusCodeApproved}|g" ${file}
    sed -i "s|\${ISSUE_STATUS_CODE_REVIEW}|${issueStatusCodeReview}|g" ${file}
    sed -i "s|\${DOCKER_HOST}|${dockerHost}|g" ${file}
}

createJob() {
    local name=${1}
    local repo=${2}
    local credentialId=${3}
    local jobDir=$(jobDir ${name})
    echo "Creating job '${name}' from repository '${repo}' with SSH credential '${credentialId}'..."
    mkdir -p ${jobDir}
    cp /files/template-job-config.xml ${jobDir}/config.xml
    sed -i "s|REPO|${repo}|g" ${jobDir}/config.xml
    sed -i "s|CREDENTIAL_ID|${credentialId}|g" ${jobDir}/config.xml
}

jobDir() {
    local name=${1}
    local jobDir="${JENKINS_HOME}/jobs/${name}"
    echo -n ${jobDir}
}

createJobs() {
    local repositories=${1}
    local name url credentialId
    for repository in ${repositories}; do
        name=$(echo ${repository} | cut -d';' -f1)
        url=$(echo ${repository} | cut -d';' -f2)
        credentialId=$(echo ${repository} | cut -d';' -f3)
        createJob "${name}" "${url}" "${credentialId}"
    done
}

createUserPassCredential() {
    local id=${1}
    local filePrefix=${2-${id}}
    local username password
    local credentialFile=${JENKINS_HOME}/credentials.xml
    echo "Adding username/password credential '${id}'"
    cat /files/template-credentials-userpass-entry.xml >> ${credentialFile} || return 1
    sed -i "s|CREDENTIAL_ID|${id}|g" ${credentialFile} || return 1
    username=$(cat /run/secrets/${filePrefix}_username) || return 1
    password=$(cat /run/secrets/${filePrefix}_password) || return 1
    sed -i "s|USERNAME|${username}|g" ${credentialFile} || return 1
    sed -i "s|PASSWORD|${password}|g" ${credentialFile} || return 1
}

createSecretStringCredential() {
    local id=${1}
    local fileName=${2-${id}}
    local secret
    local credentialFile=${JENKINS_HOME}/credentials.xml
    echo "Adding secret string credential '${id}'"
    cat /files/template-credentials-secretstring-entry.xml >> ${credentialFile} || return 1
    sed -i "s|CREDENTIAL_ID|${id}|g" ${credentialFile} || return 1
    secret=$(cat /run/secrets/${fileName}) || return 1
    sed -i "s|SECRET_STRING|${secret}|g" ${credentialFile} || return 1
}

createSshKeyCredential() {
    local keyFile=${1}
    local credentialFile=${JENKINS_HOME}/credentials.xml
    echo "Adding SSH key credential '${keyFile}'"
    cat /files/template-credentials-sshkey-entry.xml >> ${credentialFile} || return 1
    sed -i "s|CREDENTIAL_ID|${keyFile##*/}|g" ${credentialFile} || return 1
    sed -i "s|CREDENTIAL_FILE|${keyFile}|g" ${credentialFile} || return 1
}

createCredentials() {
    local credentialFile=${JENKINS_HOME}/credentials.xml
    cat /files/credentials-header.xml > ${credentialFile} || return 1
    for sshKeyFile in $(find /run/secrets -type f -name ssh.*); do createSshKeyCredential ${sshKeyFile}; done
    createUserPassCredential 'crucible'
    createUserPassCredential 'nexus'
    createUserPassCredential 'jira'
    createUserPassCredential 'artifactory-publish' 'artifactory'
    createUserPassCredential 'aws'
    createUserPassCredential 'dockerHub'
    createSecretStringCredential 'artifactory' 'artifactory-cleaner'
    cat /files/credentials-footer.xml >> ${credentialFile} || return 1
}

createDockerCredentials() {
    echo "Adding Docker credentials for TLS"
    mkdir /docker || return 1
    cp /run/secrets/docker-ca.agent /docker/ca.pem || return 1
    cp /run/secrets/docker-key.agent /docker/key.pem || return 1
    cp /run/secrets/docker-cert.agent /docker/cert.pem || return 1
    chmod -R a+r /docker || return 1
}

createJiraConfiguration() {
    local file="${JENKINS_HOME}/org.thoughtslive.jenkins.plugins.jira.Config.xml"
    echo "Adding JIRA configuration"
    cat /files/template-jira-basic.xml > ${file} || return 1
    jira_username=$(cat /run/secrets/jira_username) || return 1
    jira_password=$(cat /run/secrets/jira_password) || return 1
    sed -i "s|USERNAME|${jira_username}|g" ${file} || return 1
    sed -i "s|PASSWORD|${jira_password}|g" ${file} || return 1
    sed -i "s|URL|${JIRA_URL}|g" ${file} || return 1
}

createSshKnownHosts() {
    mkdir -p ${JENKINS_HOME}/.ssh
    echo "${KNOWN_HOSTS}" > ${JENKINS_HOME}/.ssh/known_hosts
    chmod 600 ${JENKINS_HOME}/.ssh/known_hosts
}

addgroup -g ${gid} jenkins && adduser -h "${JENKINS_HOME}" -u ${uid} -G jenkins -s /bin/bash -D jenkins

cp /files/scriptApproval.xml ${JENKINS_HOME}
cp /files/hudson.plugins.emailext.ExtendedEmailPublisher.xml ${JENKINS_HOME}
cp /files/jenkins.model.JenkinsLocationConfiguration.xml ${JENKINS_HOME}
ln -s /plugins ${JENKINS_HOME}/plugins
chown -R ${uid}:${gid} /plugins

createConfiguration ${ISSUE_STATUS_CODE_APPROVED} ${ISSUE_STATUS_CODE_REVIEW} ${ISSUE_STATUS_MANUAL_VERIFICATION} ${ISSUE_STATUS_MANUAL_VERIFICATION_OK} ${DOCKER_HOST}
createJobs ${REPOSITORIES} || exit 1
createCredentials || exit 1
createDockerCredentials || exit 1
createJiraConfiguration || exit 1
createSshKnownHosts || exit 1
chown -R ${uid}:${gid} ${JENKINS_HOME}

