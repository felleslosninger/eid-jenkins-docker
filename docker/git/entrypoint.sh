#!/usr/bin/env bash

if [ -d /run/secrets ]; then
    ssh_dir=/home/git/.ssh
    for key in $(find /run/secrets -type f); do
        echo "Adding ${key} to authorized keys"
        cp ${key} /tmp/key
        chmod 400 /tmp/key
        ssh-keygen -y -f /tmp/key >> ${ssh_dir}/authorized_keys
        rm /tmp/key
    done
    chown -R git:git ${ssh_dir}
    chmod 700 ${ssh_dir}
    chmod -R 400 ${ssh_dir}/*
fi

/usr/sbin/sshd -D
