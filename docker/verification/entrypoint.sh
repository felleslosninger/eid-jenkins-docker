#!/usr/bin/env bash

timestamp() {
    date -Iseconds
}

waitForService() {
    local host=$1
    local port=$2
    echo "$(timestamp) Waiting for service $host:$port to be available..."
    while ! nc -z ${host} ${port} >/dev/null; do
        sleep 0.5
    done
    echo "$(timestamp) Service $host:$port is available"
}

eval $(ssh-agent)
for key in $(find /run/secrets -type f); do
    echo "Adding ${key} to ssh-agent"
    cp ${key} /tmp/key
    chmod 400 /tmp/key
    ssh-add /tmp/key
    rm /tmp/key
done

mkdir -p ~/.ssh
waitForService git 22
ssh-keyscan git > ~/.ssh/known_hosts

ssh git@git create-repository verification.git || exit 1
rm -rf /tmp/verification
git clone git@git:verification /tmp/verification || exit 1
cd /tmp/verification
touch README.md
git add .
git config user.email "test@example.com"
git commit -m initial
git push origin master
git checkout -b work/TEST-1234 || exit 1
cat > pom.xml << EOF
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                      http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test-app</artifactId>
    <packaging>jar</packaging>
    <version>DEV-SNAPSHOT</version>
</project>
EOF
cat > Jenkinsfile << EOF
pipelineWithMavenAndDocker {
    gitSshKey = 'ssh.github.com'
    stagingEnvironment = 'staging'
}
EOF
git add pom.xml Jenkinsfile
git commit -mready\! || exit 1
git push -u origin work/TEST-1234 || exit 1

waitForService jenkins 8080 || exit 1
waitForService jira 80 || exit 1
groovy -cp / /verify || exit 1
