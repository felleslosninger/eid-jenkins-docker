#!/usr/bin/env bash
NEW_VERSION=$1
if [ "${NEW_VERSION}" = "" ]
then
echo "No version specified, getting latest"
NEW_VERSION=$(curl -s http://updates.jenkins-ci.org/current/latestCore.txt)
fi
echo "New version is  ${NEW_VERSION}"
SHA1=$(curl -s https://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/${NEW_VERSION}/jenkins-war-${NEW_VERSION}.war.sha1)
echo "SHA of new version is  ${SHA1}"
sed -i "s/^ARG JENKINS_VERSION=.*/\ARG JENKINS_VERSION=${NEW_VERSION}/g" Dockerfile
sed -i "s/^ARG JENKINS_SHA.*/\ARG JENKINS_SHA=${SHA1}/g" Dockerfile