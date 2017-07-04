#!/usr/bin/env bash

verify() {
    docker build -t $(__registry)/eid-jenkins:DEV-SNAPSHOT .
}

deliver() {
    version=$1
    username=$2
    password=$3
    docker build -t $(__registry)/eid-jenkins:${version} . || { echo "Build failed"; return 1; }
    docker login $(__registry) -u "${username}" -p "${password}"
    docker push $(__registry)/eid-jenkins:${version} || { echo "Delivery failed"; return 1; }
    docker logout $(__registry)
}

__registry() {
    echo -n 'eid-jenkins02.dmz.local:8081'
}

case $1 in
    verify)
        shift
        verify
        ;;
    deliver)
        shift
        deliver ${@}
        ;;
esac
