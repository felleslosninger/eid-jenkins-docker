#!/usr/bin/env bash

[[ $# -eq 1 ]] || { >&2 echo "Usage: $0 JENKINS_BASE_URL"; exit 1; }
jenkinsBaseUrl=$1
rootDir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tmpFile=$(mktemp)
currentPlugins=$(${rootDir}/utils/get-current-plugins ${jenkinsBaseUrl}) || exit 1
for plugin in ${currentPlugins}; do
    echo "${plugin/:/ }" >> ${tmpFile}
done
mv ${tmpFile} "${rootDir}/docker/jenkins-plugins/plugins.txt"

