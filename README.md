# Jenkins i Docker-konteiner

Dette er en prekonfigurert Jenkins-instans som benytter pipeline-as-code-konseptet.

## Hvordan kjøre konteineren

```
$ docker run -d \
  -p 80:8080 \
  -v ~jenkins/.docker/key.pem:/tmp/docker_key \
  -v ~jenkins/.ssh/id_rsa:/tmp/git_key \
  -v ~jenkins/.m2/repository:/maven-repo \
  -e uid=`id -u jenkins` \
  -e gid=`id -g jenkins` \
  --restart=unless-stopped \
  --name jenkins \
  docker-registry.dmz.local/eid-jenkins
```

## Hvordan vedlikeholde bildet

### Oppgradere Jenkins-versjon

Dockerfile inneholder følgende linjer:

```
ARG JENKINS_VERSION=2.13
ARG JENKINS_SHA=3eb1a8e9bf396a56595b75449ba245012287f4dd
```

På http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/<versjon>/jenkins-war-<versjon>.war finner du siste versjon, og SHA1-sum for denne finner du i http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/<versjon>/jenkins-war-<versjon>.war.sha1. Erstatt argumentene over med disse for å bygge bilder med denne versjonen.
