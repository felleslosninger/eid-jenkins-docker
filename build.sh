#!/usr/bin/env bash

verify() {
    docker build -t $(__registry)/eid-jenkins:DEV-SNAPSHOT .
}

deliver() {
    version=$1
    docker build -t $(__registry)/eid-jenkins:${version} . || { echo "Build failed"; return 1; }
    docker push $(__registry)/eid-jenkins:${version} || { echo "Delivery failed"; return 1; }
}

__registry() {
    echo -n 'eid-jenkins02.dmz.local:8081'
}

case $1 in
    verify)
        verify
        ;;
    deliver)
        deliver $2
        ;;
esac
