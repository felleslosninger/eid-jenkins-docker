#! /bin/bash

addgroup -g ${gid} jenkins && adduser -h "${JENKINS_HOME}" -u ${uid} -G jenkins -s /bin/bash -D jenkins

cd ${JENKINS_HOME}

cp -r /files/plugins ${JENKINS_HOME}

cp /files/scriptApproval.xml .

createJob() {
    name=$1
    repo=$2
    jobDir=jobs/${name}
    [ ! -e ${jobDir} ] && mkdir -p ${jobDir}
    cp /files/template-config.xml ${jobDir}/config.xml
    sed -i "s|REPO|${repo}|g" ${jobDir}/config.xml
}

createJob eid git@git.difi.local:eid
createJob jenkins-docker https://github.com/difi/jenkins-docker
createJob kontaktregister-statistikk https://github.com/difi/kontaktregister-statistikk
createJob poc-statistics https://github.com/difi/poc-statistics
createJob eid-oidc-provider git@git.difi.local:eid-oidc-provider

chown -R jenkins:jenkins ${JENKINS_HOME}
