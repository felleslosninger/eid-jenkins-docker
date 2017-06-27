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

declare -A credentialIds
for repository in ${REPOSITORIES}; do
    name=$(echo ${repository} | cut -d';' -f1)
    url=$(echo ${repository} | cut -d';' -f2)
    credentialId=$(echo ${repository} | cut -d';' -f3)
    createJob "${name}" "${url}" "${credentialId}"
    [ ! -z "${credentialId}" ] && credentialIds["${credentialId}"]=1
done

cat /files/credentials-header.xml > credentials.xml || exit 1
for credentialId in "${!credentialIds[@]}"; do
    echo "Adding credentials id ${credentialId}"
    cat /files/template-credentials-sshkey-entry.xml >> credentials.xml || exit 1
    sed -i "s|CREDENTIAL_ID|${credentialId}|g" credentials.xml || exit 1
done
echo "Adding credentials id crucible"
cat /files/template-credentials-userpass-entry.xml >> credentials.xml || exit 1
sed -i "s|CREDENTIAL_ID|crucible|g" credentials.xml || exit 1
crucible_username=$(cat /run/secrets/crucible_username) || exit 1
crucible_password=$(cat /run/secrets/crucible_password) || exit 1
sed -i "s|USERNAME|${crucible_username}|g" credentials.xml || exit 1
sed -i "s|PASSWORD|${crucible_password}|g" credentials.xml || exit 1
cat /files/credentials-footer.xml >> credentials.xml || exit 1

mkdir ${JENKINS_HOME}/.ssh
echo "${KNOWN_HOSTS}" > ${JENKINS_HOME}/.ssh/known_hosts
chmod 600 ${JENKINS_HOME}/.ssh/known_hosts

chown -R ${uid}:${gid} ${JENKINS_HOME}
