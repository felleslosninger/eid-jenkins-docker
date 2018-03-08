#!/usr/bin/env bash

[ -d /run/secrets ] && {
    for secretFile in /run/secrets/*; do
        echo "${secretFile##*/}=$(cat ${secretFile})" >> /application.properties
    done
}

java --add-modules jdk.incubator.httpclient -jar /polling-agent.jar --server.port=80