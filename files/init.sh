#! /bin/bash

addgroup -g ${gid} jenkins && adduser -h "${JENKINS_HOME}" -u ${uid} -G jenkins -s /bin/bash -D jenkins

cd ${JENKINS_HOME}

cp /files/scriptApproval.xml .
cp /files/hudson.plugins.emailext.ExtendedEmailPublisher.xml .
cp /files/jenkins.model.JenkinsLocationConfiguration.xml .

createJob() {
    local name=$1
    local repo=$2
    local jobDir=$(jobDir ${name})
    [ ! -e ${jobDir} ] && mkdir -p ${jobDir}
    cp /files/template-config.xml ${jobDir}/config.xml
    sed -i "s|REPO|${repo}|g" ${jobDir}/config.xml
}

jobDir() {
    name=$1
    local jobDir="jobs/${name}"
    echo -n ${jobDir}
}

createJob eid git@git.difi.local:eid
createJob jenkins-docker https://github.com/difi/jenkins-docker
createJob kontaktregister-statistikk https://github.com/difi/kontaktregister-statistikk
createJob poc-statistics https://github.com/difi/poc-statistics
createJob eid-oidc-provider git@git.difi.local:eid-oidc-provider
createJob minid-on-the-fly git@git.difi.local:minid-on-the-fly
createJob eid-resilience git@git.difi.local:eid-common-resilience.git
createJob eid-common git@git.difi.local:eid-common.git
createJob krr git@git.difi.local:krr.git
createJob idporten git@git.difi.local:idporten.git
createJob idporten-admin git@git.difi.local:idporten-admin.git
createJob idporten-authlevel git@git.difi.local:idporten-authlevel.git
createJob idporten-app-dpi-reklame git@git.difi.local:idporten-app-dpi-reklame.git
createJob idporten-minid-updater git@git.difi.local:idporten-minid-updater.git
createJob idporten-pinkoder git@git.difi.local:idporten-pinkoder.git
createJob dsf-gateway git@git.difi.local:dsf-gateway.git
createJob eid-level1-poc git@git.difi.local:eid-level1-poc.git
createJob eid-system-tests git@git.difi.local:eid-system-tests.git

chown -R jenkins:jenkins ${JENKINS_HOME}
