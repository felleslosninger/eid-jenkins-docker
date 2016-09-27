#! /bin/bash

REF=/var/jenkins_home/plugins
mkdir -p $REF

name=$1
version=$2

echo "Downloading $name:$version"

if [ -z "$JENKINS_UC_DOWNLOAD" ]; then
  JENKINS_UC_DOWNLOAD=$JENKINS_UC/download
fi
curl -sSL -f ${JENKINS_UC_DOWNLOAD}/plugins/$name/$version/$name.hpi -o $REF/$name.jpi
unzip -qqt $REF/$name.jpi
