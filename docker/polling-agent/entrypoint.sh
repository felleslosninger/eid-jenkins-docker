#!/usr/bin/env bash

[ -d /run/secrets ] && {
    for secretFile in /run/secrets/*; do
        echo "${secretFile##*/}=$(cat ${secretFile})" >> /opt/polling-agent/application.properties
    done
}
java -jar /opt/polling-agent/polling-agent.jar 