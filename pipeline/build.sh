#!/usr/bin/env bash

verify() {
    docker build -t docker-registry.dmz.local/eid-jenkins:DEV-SNAPSHOT .
}

deliver() {
    version=$1
    docker build -t docker-registry.dmz.local/eid-jenkins:${version} .
    docker push docker-registry.dmz.local/eid-jenkins:${version}
}

case $1 in
    verify)
        verify
        ;;
    deliver)
        deliver $2
        ;;
esac
