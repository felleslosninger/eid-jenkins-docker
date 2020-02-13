#!/usr/bin/env bash

export repositoryDirectory=target/data
export jiraUsername=randomUsername
export jiraPassword=random
#NOT ALLOWED in bash to user enviroment variables with period like jira.username and jira.password

mvn compile spring-boot:run