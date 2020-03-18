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

# first verification repo
ssh git@git create-repository verification1.git || exit 1
rm -rf /tmp/verification1
git clone git@git:verification1 /tmp/verification1 || exit 1
cd /tmp/verification1
touch README.md
git add .
git config user.email "test@example.com"
git commit -m initial
git push origin master
git checkout -b work/TEST-1234 || exit 1
cp /tmp/project/* .
git add *
git commit -mready\! || exit 1
git push -u origin work/TEST-1234 || exit 1

# second verification repo - not working because of mocking of jira is not roboust enough to handle several issues
#ssh git@git create-repository verification2.git || exit 1
#rm -rf /tmp/verification2
#git clone git@git:verification2 /tmp/verification2 || exit 1
#cd /tmp/verification2
#echo "A Readme: verification2" >> README.md
#git add .
#git config user.email "test@example.com"
#git commit -m initial
#git push origin master
#git checkout -b work/TEST-1000 || exit 1
#cp /tmp/project/* .
#echo "A Readme: verification2" > README.md
#git add *
#git commit -mready\! || exit 1
#git push -u origin work/TEST-1000 || exit 1

waitForService jenkins 8080 || exit 1
waitForService jira 80 || exit 1
groovy -cp / /verify || exit 1
