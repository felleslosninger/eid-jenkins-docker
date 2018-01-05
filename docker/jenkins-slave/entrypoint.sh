#!/usr/bin/env bash

curl -fsS --retry 100 --retry-delay 5 http://jenkins:8080/jnlpJars/agent.jar -o /tmp/agent.jar || exit 1
groovy /files/create-ssh-known-hosts /config.yaml || exit 1
java -jar /tmp/agent.jar -jnlpUrl http://jenkins:8080/computer/${SERVICE}/slave-agent.jnlp -workDir "/tmp"