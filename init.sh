#! /bin/bash

cp /tmp/git_key $JENKINS_HOME/git_key
cp /tmp/docker_key $JENKINS_HOME/.docker/key.pem
chown jenkins:jenkins \
  $JENKINS_HOME/git_key \
  $JENKINS_HOME/.docker/key.pem
