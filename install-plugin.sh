#! /bin/bash

name=$1
version=$2

installDir=/files/plugins
downloadBaseUrl=https://updates.jenkins.io/download/plugins

mkdir -p ${installDir}

echo "Downloading plugin ${name}:${version}..."

curl -sSL -f ${downloadBaseUrl}/${name}/${version}/${name}.hpi -o ${installDir}/${name}.jpi
unzip -qqt ${installDir}/${name}.jpi
