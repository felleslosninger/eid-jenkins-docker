#!/usr/bin/env bash

[[ $# -eq 1 ]] || { >&2 echo "Usage: $0 JENKINS_BASE_URL"; exit 1; }
jenkinsBaseUrl=$1
rootDir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
dockerFile="${rootDir}/docker/jenkins-plugins/Dockerfile"
tmpFile=$(mktemp)
cat ${dockerFile} | grep -v "RUN install-plugin.sh" > ${tmpFile}
currentPlugins=$(${rootDir}/utils/get-current-plugins ${jenkinsBaseUrl}) || exit 1
for plugin in ${currentPlugins}; do
    echo "RUN install-plugin.sh ${plugin/:/ }" >> ${tmpFile}
done
mv ${tmpFile} ${dockerFile}

