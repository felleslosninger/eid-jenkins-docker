#! /bin/bash

addgroup -g ${gid} jenkins && adduser -h "${JENKINS_HOME}" -u ${uid} -G jenkins -s /bin/bash -D jenkins

cd ${JENKINS_HOME}

cp -r /files/plugins ${JENKINS_HOME}

cp /files/scriptApproval.xml .
cp /files/hudson.plugins.emailext.ExtendedEmailPublisher.xml .
cp /files/jenkins.model.JenkinsLocationConfiguration.xml .

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
createJob minid-on-the-fly git@git.difi.local:minid-on-the-fly
createJob resilience git@git.difi.local:eid-common-resilience.git
createJob idporten-authlevel git@git.difi.local:idporten-authlevel.git
createJob puppet-hiera git@eid-gitlab.dmz.local:puppet/puppet_hiera.git
createJob puppet-control git@eid-gitlab.dmz.local:puppet/puppet_control.git

chown -R jenkins:jenkins ${JENKINS_HOME}
