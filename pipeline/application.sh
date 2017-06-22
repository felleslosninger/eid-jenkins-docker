#!/usr/bin/env bash

fail() {
    ret=$?
    message=${1-"[Failed (${ret})]"}
    echo ${message}
    return ${ret}
}

warn() {
    message=$1
    echo ${message}
}

ok() {
    echo "[OK]"
}

dotSleep() {
    length=${1-1}
    echo -n "."
    sleep ${length};
}

requireArgument() {
    test -z ${!1} && fail "Missing argument '${1}'"
}

image() {
    service=$1
    version=${2-'latest'}
    requireArgument 'service'
    case "${service}" in
        "jenkins")
            image="docker-registry.dmz.local/eid-jenkins:${version}"
            ;;
        "selenium")
            image="selenium/standalone-firefox:${version}"
            ;;
        *)
            fail "Unknown service ${service}"
    esac
    echo ${image}
}

createService() {
    service=$1
    version=${2-'latest'}
    requireArgument 'service'
    network='pipeline'
    echo -n "Creating service ${service} of version ${version}... "
    image=$(image ${service} ${version})
    case ${service} in
    jenkins)
        output=$(sudo docker service create \
            --network ${network} \
            --constraint 'node.role == manager' \
            --mount type=bind,src=$SSH_AUTH_SOCK,target=/ssh_auth_sock \
            --mount type=bind,src=$(eval echo ~$USER)/data,target=/var/jenkins_home \
            --mount type=bind,src=$(eval echo ~$USER)/.docker,target=/var/jenkins_home/.docker \
            --mount type=bind,src=$(eval echo ~$USER)/.m2,target=/var/jenkins_home/.m2 \
            --mount type=bind,src=$(eval echo ~$USER)/.aws,target=/var/jenkins_home/.aws \
            --detach=false \
	        --secret git_difi \
            --secret artifactory-cleaner \
            --secret crucible_username \
            --secret crucible_password \
            -e DOCKER_HOST=tcp://$(hostname -f):2376 \
            -e uid=`id -u $USER` \
            -e gid=`id -g $USER` \
            --name ${service} \
            -p 80:8080 \
            ${image}) \
            || fail "Failed to create service ${service}"
        ;;
    selenium)
        output=$(sudo docker service create \
            --network ${network} \
            --detach=false \
            --name selenium_host \
            -p 4444:4444 \
            ${image}) \
            || fail "Failed to create service ${service}"
        ;;
    esac
    ok
}

updateService() {
    service=$1
    version=${2-'latest'}
    requireArgument 'service'
    echo -n "Updating service ${service} to version ${version}... "
    image=$(image ${service} ${version})
    output=$(sudo docker service inspect ${service}) || { echo "Service needs to be created"; createService ${service} ${version}; return; }
    output=$(sudo docker service update --image ${image} ${service}) \
        && ok || fail
}

waitForServiceUpdateToComplete() {
    service=$1
    requireArgument 'service'
    echo -n "Waiting for service \"${service}\" to be updated"
    for i in $(seq 1 100); do isServiceUpdateCompleted ${service} && status=true && break || dotSleep; done
    echo -n " "
    ${status} && ok || fail
}

isServiceUpdateCompleted() {
    service="${1}"
    requireArgument 'service'
    [ "$(sudo docker service inspect ${service} -f '{{.UpdateStatus.State}}')" == "completed" ]
}

deleteService() {
    service=$1
    requireArgument 'service'
    echo -n "Deleting service ${service}... "
    output=$(sudo docker service rm ${service}) \
        && ok || fail
}

createNetwork() {
    network=$1
    requireArgument 'network'
    echo -n "Creating network ${network}... "
    output=$(sudo docker network create -d overlay --attachable --subnet 10.0.1.0/24 ${network}) \
        && ok || fail
}

deleteNetwork() {
    network=$1
    requireArgument 'network'
    echo -n "Deleting network ${network}... "
    output=$(sudo docker network rm ${network}) \
        && ok || fail
}

waitForServiceToBeAvailable() {
    service=$1
    requireArgument 'service'
    host=${2-'localhost'}
    echo -n "Waiting for service \"${service}\" to be available: "
    status=false
    for i in $(seq 1 200); do
        isServiceAvailable ${service} ${host}
        ret=$?
        [ ${ret} -eq 7 -o ${ret} -eq 27 ] && dotSleep; # Connect failure or request timeout
        [ ${ret} -eq 0 ] && { status=true; break; }
        [ ${ret} -eq 1 ] && break # Unknown service
    done
    ${status} && ok || fail
}

isServiceAvailable() {
    service=$1
    requireArgument 'service'
    host=${2-'localhost'}
    case "${service}" in
        'jenkins')
            url="http://${host}:80"
            ;;
        *)
            echo -n "Unknown service \"${service}\""
            return 1
    esac
    curl -s ${url} --connect-timeout 3 --max-time 10 > /dev/null
}

create() {
    version=${1-'latest'}
    echo "Creating application with version ${version}..."
    createNetwork 'pipeline' || return $?
    createService 'jenkins' ${version} || return $?
    createService 'selenium' 2.53.0 || return $?
    echo "Application created"
}

update() {
    version=${1-'latest'}
    echo "Updating application to version ${version}..."
    updateService 'jenkins' ${version} || return $?
    updateService 'selenium' 2.53.0 || return $?
    echo "Application updated"
}

delete() {
    echo "Deleting application..."
    deleteService "jenkins"
    deleteService "selenium"
    deleteNetwork "pipeline"
    echo "Application deleted"
}

case "${1}" in *)
        function="${1}"
        shift
        ${function} "${@}"
        ;;
esac
