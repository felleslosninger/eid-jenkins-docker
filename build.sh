#!/usr/bin/env bash

verify() {
    __build jenkins DEV-SNAPSHOT
    __build jenkins-plugins DEV-SNAPSHOT
}

deliver() {
    [[ $# -eq 3 ]] || { >&2 echo "Usage: $0 VERSION USERNAME PASSWORD"; return 1; }
    version=$1
    username=$2
    password=$3
    docker login $(__registry) -u "${username}" -p "${password}"
    __build jenkins-plugins ${version} || return 1
    __push jenkins-plugins ${version} || return 1
    __build jenkins ${version} || return 1
    __push jenkins ${version} || return 1
    docker logout $(__registry)
}

__build() {
    local imageName=$1
    local version=$2
    cd "${ROOT}/${imageName}"
    docker build -t $(__registry)/${imageName}:${version} . || { echo "Failed to build ${imageName}"; return 1; }
}

__push() {
    local imageName=$1
    local version=$2
    docker push $(__registry)/${imageName}:${version} || { echo "Failed to deliver ${imageName}"; return 1; }
}

__registry() {
    echo -n 'eid-jenkins02.dmz.local:8081'
}

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

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
