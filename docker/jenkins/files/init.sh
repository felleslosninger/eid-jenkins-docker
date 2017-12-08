#!/usr/bin/env bash

createConfiguration() {
    local file=${JENKINS_HOME}/config.xml
    cp /files/template-config.xml ${file}
    sed -i "s|\${ISSUE_STATUS_OPEN}|${1}|g" ${file} && shift
    sed -i "s|\${ISSUE_STATUS_IN_PROGRESS}|${1}|g" ${file} && shift
    sed -i "s|\${ISSUE_STATUS_CODE_APPROVED}|${1}|g" ${file} && shift
    sed -i "s|\${ISSUE_STATUS_CODE_REVIEW}|${1}|g" ${file} && shift
    sed -i "s|\${ISSUE_STATUS_MANUAL_VERIFICATION}|${1}|g" ${file} && shift
    sed -i "s|\${ISSUE_STATUS_MANUAL_VERIFICATION_OK}|${1}|g" ${file} && shift
    sed -i "s|\${ISSUE_TRANSITION_START}|${1}|g" ${file} && shift
    sed -i "s|\${ISSUE_TRANSITION_READY_FOR_CODE_REVIEW}|${1}|g" ${file} && shift
    sed -i "s|\${ISSUE_TRANSITION_RESUME_WORK}|${1}|g" ${file} && shift
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

migrateLegacyBuildDirectories() {
    local sources
    sources=$(find ${JENKINS_HOME}/jobs/ -path "*/branches/*/builds" -type d) || return 1
    for source in ${sources}; do target=/builds/$(echo "${source}" | cut -d/ -f5,7) && echo "Migrating legacy build location ${source} to ${target}" && mkdir -p ${target} && mv -n ${source}/{.,}* ${target}; rm -rf ${source}; done
}

addgroup -g ${gid} jenkins && adduser -h "${JENKINS_HOME}" -u ${uid} -G jenkins -s /bin/bash -D jenkins

cp /files/scriptApproval.xml ${JENKINS_HOME}
cp /files/hudson.plugins.emailext.ExtendedEmailPublisher.xml ${JENKINS_HOME}
cp /files/jenkins.model.JenkinsLocationConfiguration.xml ${JENKINS_HOME}
cp /files/org.jenkinsci.plugins.workflow.libs.GlobalLibraries.xml ${JENKINS_HOME}
ln -s /plugins ${JENKINS_HOME}/plugins
chown -R ${uid}:${gid} /plugins

createConfiguration \
    "${ISSUE_STATUS_OPEN}" \
    "${ISSUE_STATUS_IN_PROGRESS}" \
    "${ISSUE_STATUS_CODE_APPROVED}" \
    "${ISSUE_STATUS_CODE_REVIEW}" \
    "${ISSUE_STATUS_MANUAL_VERIFICATION}" \
    "${ISSUE_STATUS_MANUAL_VERIFICATION_OK}" \
    "${ISSUE_TRANSITION_START}" \
    "${ISSUE_TRANSITION_READY_FOR_CODE_REVIEW}" \
    "${ISSUE_TRANSITION_RESUME_WORK}"
groovy /files/create-jobs.groovy /jobs.yaml || exit 1
groovy /files/create-slaves.groovy ${JENKINS_SLAVES} || exit 1
createCredentials || exit 1
createDockerCredentials || exit 1
createJiraConfiguration || exit 1
createSshKnownHosts || exit 1
chown -R ${uid}:${gid} ${JENKINS_HOME}
chown ${uid}:${gid} /workspaces
chown ${uid}:${gid} /builds
migrateLegacyBuildDirectories || exit 1
