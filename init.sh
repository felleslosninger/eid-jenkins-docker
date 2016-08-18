#! /bin/bash

cp /tmp/git_key $JENKINS_HOME/git_key
cp /tmp/docker_key $JENKINS_HOME/.docker/key.pem
cp /tmp/key_saml_metadata_validator $JENKINS_HOME/key_saml_metadata_validator

addgroup -g ${gid} jenkins && adduser -h "$JENKINS_HOME" -u ${uid} -G jenkins -s /bin/bash -D jenkins
chown -R jenkins:jenkins $JENKINS_HOME

su jenkins
git config --global user.email "eid-jenkins@difi.no"
git config --global user.name "eid-jenkins"