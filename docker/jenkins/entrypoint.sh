#!/usr/bin/env bash

createUserPassCredential() {
    local id=${1}
    local filePrefix=${2-${id}}
    local username password
    local credentialFile=${JENKINS_HOME}/credentials.xml
    echo "Adding username/password credential '${id}'"
    cat /templates/credentials-userpass-entry.xml >> ${credentialFile} || return 1
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
    cat /templates/credentials-secretstring-entry.xml >> ${credentialFile} || return 1
    sed -i "s|CREDENTIAL_ID|${id}|g" ${credentialFile} || return 1
    secret=$(cat /run/secrets/${fileName}) || return 1
    sed -i "s|SECRET_STRING|${secret}|g" ${credentialFile} || return 1
}

createSshKeyCredential() {
    local keyFile=${1}
    local credentialFile=${JENKINS_HOME}/credentials.xml
    echo "Adding SSH key credential '${keyFile}'"
    cat /templates/credentials-sshkey-entry.xml >> ${credentialFile} || return 1
    sed -i "s|CREDENTIAL_ID|${keyFile##*/}|g" ${credentialFile} || return 1
    sed -i "s|CREDENTIAL_FILE|${keyFile}|g" ${credentialFile} || return 1
}

createCredentials() {
    local credentialFile=${JENKINS_HOME}/credentials.xml
    cat /templates/credentials-header.xml > ${credentialFile} || return 1
    for sshKeyFile in $(find /run/secrets -type f -name ssh.*); do createSshKeyCredential ${sshKeyFile}; done
    for f in $(find /run/secrets -type f -name docker_registry_*_username); do createUserPassCredential $(basename ${f//_username}); done
    createUserPassCredential 'crucible'
    createUserPassCredential 'nexus'
    createUserPassCredential 'jira'
    createUserPassCredential 'artifactory-publish' 'artifactory'
    createUserPassCredential 'aws'
    createUserPassCredential 'dockerHub'
    createSecretStringCredential 'artifactory' 'artifactory-cleaner'
    createSecretStringCredential 'gitlab-api'
    cat /templates/credentials-footer.xml >> ${credentialFile} || return 1
}

cp /static/* ${JENKINS_HOME}
ln -s /plugins ${JENKINS_HOME}/plugins

groovy /scripts/create-config /config.yaml /templates/config.xml || exit 1
groovy /scripts/create-location-configuration /config.yaml /templates/jenkins.model.JenkinsLocationConfiguration.xml || exit 1
groovy /scripts/create-jira-config /config.yaml /templates/jira-basic.xml || exit 1
groovy /scripts/create-jobs /jobs.yaml /templates/job-config.xml || exit 1
groovy /scripts/create-slaves ${JENKINS_SLAVES} || exit 1
createCredentials || exit 1
groovy /scripts/create-ssh-known-hosts /config.yaml || exit 1
groovy /scripts/create-git-config /config.yaml || exit 1

exec java -jar /usr/share/jenkins/jenkins.war --webroot=/tmp/jenkins/war --pluginroot=/tmp/jenkins/plugins
