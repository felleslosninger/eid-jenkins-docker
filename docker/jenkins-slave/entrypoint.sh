#!/usr/bin/env bash

curl -fsS http://jenkins:8080/jnlpJars/agent.jar -o /tmp/agent.jar
java -jar /tmp/agent.jar -jnlpUrl http://jenkins:8080/computer/${SERVICE}/slave-agent.jnlp -workDir "/tmp"