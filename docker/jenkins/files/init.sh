#!/usr/bin/env bash

addgroup -g ${gid} jenkins && adduser -h "${JENKINS_HOME}" -u ${uid} -G jenkins -s /bin/bash -D jenkins

cd ${JENKINS_HOME}

cp /files/scriptApproval.xml .
cp /files/hudson.plugins.emailext.ExtendedEmailPublisher.xml .
cp /files/jenkins.model.JenkinsLocationConfiguration.xml .

createJob() {
    local name=$1
    local repo=$2
    local credentialId=$3
    local jobDir=$(jobDir ${name})
    echo "Creating job '${name}' from repository '${repo}' with credentials id '${credentialId}'..."
    [ ! -e ${jobDir} ] && mkdir -p ${jobDir}
    cp /files/template-config.xml ${jobDir}/config.xml
    sed -i "s|REPO|${repo}|g" ${jobDir}/config.xml
    sed -i "s|CREDENTIAL_ID|${credentialId}|g" ${jobDir}/config.xml
}

jobDir() {
    name=$1
    local jobDir="jobs/${name}"
    echo -n ${jobDir}
}

for repository in ${REPOSITORIES}; do
    name=$(echo ${repository} | cut -d';' -f1)
    url=$(echo ${repository} | cut -d';' -f2)
    credentialId=$(echo ${repository} | cut -d';' -f3)
    createJob "${name}" "${url}" "${credentialId}"
done

cat /files/credentials-header.xml > credentials.xml || exit 1
for sshKeyFile in $(find /run/secrets -type f -name ssh.*); do
    echo "Adding SSH key from $sshKeyFile to credentials store"
    cat /files/template-credentials-sshkey-entry.xml >> credentials.xml || exit 1
    sed -i "s|CREDENTIAL_ID|${sshKeyFile##*/}|g" credentials.xml || exit 1
    sed -i "s|CREDENTIAL_FILE|${sshKeyFile}|g" credentials.xml || exit 1
done

echo "Adding credentials 'crucible'"
cat /files/template-credentials-userpass-entry.xml >> credentials.xml || exit 1
sed -i "s|CREDENTIAL_ID|crucible|g" credentials.xml || exit 1
crucible_username=$(cat /run/secrets/crucible_username) || exit 1
crucible_password=$(cat /run/secrets/crucible_password) || exit 1
sed -i "s|USERNAME|${crucible_username}|g" credentials.xml || exit 1
sed -i "s|PASSWORD|${crucible_password}|g" credentials.xml || exit 1

echo "Adding credentials 'nexus'"
cat /files/template-credentials-userpass-entry.xml >> credentials.xml || exit 1
sed -i "s|CREDENTIAL_ID|nexus|g" credentials.xml || exit 1
nexus_username=$(cat /run/secrets/nexus_username) || exit 1
nexus_password=$(cat /run/secrets/nexus_password) || exit 1
sed -i "s|USERNAME|${nexus_username}|g" credentials.xml || exit 1
sed -i "s|PASSWORD|${nexus_password}|g" credentials.xml || exit 1

echo "Adding credentials 'jira'"
cat /files/template-credentials-userpass-entry.xml >> credentials.xml || exit 1
sed -i "s|CREDENTIAL_ID|jira|g" credentials.xml || exit 1
nexus_username=$(cat /run/secrets/jira_username) || exit 1
nexus_password=$(cat /run/secrets/jira_password) || exit 1
sed -i "s|USERNAME|${jira_username}|g" credentials.xml || exit 1
sed -i "s|PASSWORD|${jira_password}|g" credentials.xml || exit 1

echo "Adding credentials 'artifactory'"
cat /files/template-credentials-secretstring-entry.xml >> credentials.xml || exit 1
sed -i "s|CREDENTIAL_ID|artifactory|g" credentials.xml || exit 1
artifactory_api_key=$(cat /run/secrets/artifactory-cleaner) || exit 1
sed -i "s|SECRET_STRING|${artifactory_api_key}|g" credentials.xml || exit 1

cat /files/credentials-footer.xml >> credentials.xml || exit 1

mkdir ${JENKINS_HOME}/.ssh
echo "${KNOWN_HOSTS}" > ${JENKINS_HOME}/.ssh/known_hosts
chmod 600 ${JENKINS_HOME}/.ssh/known_hosts

chown -R ${uid}:${gid} ${JENKINS_HOME}
