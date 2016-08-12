#! /bin/bash

cp /tmp/git_key $JENKINS_HOME/git_key
cp /tmp/docker_key $JENKINS_HOME/.docker/key.pem
cp /tmp/id_rsa_metadata_validator $JENKINS_HOME/id_rsa_metadata_validator

addgroup -g ${gid} jenkins && adduser -h "$JENKINS_HOME" -u ${uid} -G jenkins -s /bin/bash -D jenkins
chown -R jenkins:jenkins $JENKINS_HOME
