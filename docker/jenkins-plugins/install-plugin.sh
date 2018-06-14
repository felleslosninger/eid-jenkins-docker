#! /bin/bash

name=$1
version=$2

installDir=/plugins
downloadBaseUrl=http://ftp-nyc.osuosl.org/pub/jenkins/plugins

mkdir -p ${installDir}

echo "Downloading plugin ${name}:${version}..."

curl -sSL -f ${downloadBaseUrl}/${name}/${version}/${name}.hpi -o ${installDir}/${name}.hpi
