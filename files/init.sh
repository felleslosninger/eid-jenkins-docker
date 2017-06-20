#! /bin/bash

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

read -r -d '' REPOSITORIES << EOF
eid;git@git.difi.local:eid;git_difi;
jenkins-docker;https://github.com/difi/jenkins-docker;;
kontaktregister-statistikk;https://github.com/difi/kontaktregister-statistikk;;
poc-statistics;https://github.com/difi/poc-statistics;;
eid-oidc-provider;git@git.difi.local:eid-oidc-provider;git_difi;
minid-on-the-fly;git@git.difi.local:minid-on-the-fly;git_difi;
eid-resilience;git@git.difi.local:eid-common-resilience.git;git_difi;
eid-common;git@git.difi.local:eid-common.git;git_difi;
krr;git@git.difi.local:krr.git;git_difi;
idporten;git@git.difi.local:idporten.git;git_difi;
idporten-admin;git@git.difi.local:idporten-admin.git;git_difi;
idporten-authlevel;git@git.difi.local:idporten-authlevel.git;git_difi;
idporten-app-dpi-reklame;git@git.difi.local:idporten-app-dpi-reklame.git;git_difi;
idporten-minid-updater;git@git.difi.local:idporten-minid-updater.git;git_difi;
idporten-pinkoder;git@git.difi.local:idporten-pinkoder.git;git_difi;
dine-kontaktopplysninger;git@git.difi.local:dine-kontaktopplysninger.git;git_difi;
dsf-gateway;git@git.difi.local:dsf-gateway.git;git_difi;
eid-level1-poc;git@git.difi.local:eid-level1-poc.git;git_difi;
eid-system-tests;git@git.difi.local:eid-system-tests.git;git_difi;
eid-pipeline-branch;git@git.difi.local:eid-pipeline-branch.git;git_difi;
EOF

read -r -d '' KNOWN_HOSTS << EOF
github.com ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAq2A7hRGmdnm9tUDbO9IDSwBK6TbQa+PXYPCPy6rbTrTtw7PHkccKrpp0yVhp5HdEIcKr6pLlVDBfOLX9QUsyCOV0wzfjIJNlGEYsdlLJizHhbn2mUjvSAHQqZETYP81eFzLQNnPHt4EVVUh7VfDESU84KezmD5QlWpXLmvU31/yMf+Se8xhHTvKSCZIFImWwoG6mbUoWf9nzpIoaSjB+weqqUUmpaaasXVal72J+UX2B+2RPW3RcT0eOzQgqlJL3RKrTJvdsjE3JEAvGq3lGHSZXy28G3skua2SmVi/w4yCE6gbODqnTWlg7+wC604ydGXA8VJiS5ap43JXiUFFAaQ==
git.difi.local ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBG4VYlrX7IlRq+O1lokYTp1oYe7vsIPAmm9uhDec2TVvCkPnSeNhJq9iCyRYCaaCxZLRfUjQdCNc6NvNyXdZb6k=
eid-gitlab.dmz.local ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEApaqpEGnJ/z4Ur+bgHhnpYCBCdyPuDMVxUplgVPaUTjCLEiEioU+LgZsr04pX6s9sqHbH+0+ggAizxpXQZJ4AOgyeSCwtZNpqX3jXdXMCXqjQgDY/Pxu1vi91PucNK8B4lwkrhed9Ze8iCzdKY5w1NZDpB6Auc+UL2I0HJNDIlrxenYXdqtK6udLyEv1eViGCOIPY1YJ1SjVxMWv2G4itdJDQrOqe6U3p5Ph3P810rtIh/q4Z1PbdS98Jvh3VewdMpQjqPSTskn+EsnlEGJf8vvJmsVsRol1X5bmU/mxIXsaySzPmKdH34hQFzdejdjp83FYRffV0OYhL8Ur9ZU+Apw==
eid-jenkins02.dmz.local ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBC1UgDT1mm3EJ2w90x2zeEVzxdFPxcqkp+a0N6+0WLgNgTLVFRt2/hSM48VfCaFQaOzVw8PfxVSIHtuunFry1qo=
EOF

declare -A credentialIds
for repository in ${REPOSITORIES}; do
    name=$(echo ${repository} | cut -d';' -f1)
    url=$(echo ${repository} | cut -d';' -f2)
    credentialId=$(echo ${repository} | cut -d';' -f3)
    createJob "${name}" "${url}" "${credentialId}"
    [ ! -z "${credentialId}" ] && credentialIds["${credentialId}"]=1
done

cat /files/credentials-header.xml > credentials.xml
echo "Credentials: ${!credentialIds[@]}"
for credentialId in "${!credentialIds[@]}"; do
    echo "Adding credential ${credentialId}"
    cat /files/template-credentials-entry.xml >> credentials.xml
    sed -i "s|CREDENTIAL_ID|${credentialId}|g" credentials.xml
done
cat /files/credentials-footer.xml >> credentials.xml

mkdir ${JENKINS_HOME}/.ssh
echo "${KNOWN_HOSTS}" > ${JENKINS_HOME}/.ssh/known_hosts
chmod 600 ${JENKINS_HOME}/.ssh/known_hosts

chown -R ${uid}:${gid} ${JENKINS_HOME}
