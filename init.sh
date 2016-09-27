#! /bin/bash

addgroup -g ${gid} jenkins && adduser -h "$JENKINS_HOME" -u ${uid} -G jenkins -s /bin/bash -D jenkins
chown -R jenkins:jenkins $JENKINS_HOME