# Jenkins i Docker-konteiner

Dette er en prekonfigurert Jenkins-instans som benytter pipeline-as-code-konseptet.

## Hvordan kjøre konteineren

Følgende krav stilles til vertsmaskinen:
* Docker Engine (1.12.1 eller nyere) er installert.
* Docker Engine er konfigurert for [toveis TLS-aksess](https://docs.docker.com/engine/security/https/).
* Det finnes en bruker `jenkins`.
* Brukeren har en `.docker`-katalog med nødvendige PEM-filer for å aksessere lokal Docker Engine via TLS.
* Brukeren har en fil `.ssh/known_hosts` med fingeravtrykkene til de verter Jenkins vil kontakte med SSH.
* Miljøvariabelen `SSH_AUTH_SOCK` inneholder stien til en Unix-socket for en kjørende
  [ssh-agent](https://wiki.archlinux.org/index.php/SSH_keys#SSH_agents) som holder autentiseringsnøkler for de vertene
  Jenkins vil kontakte med SSH.

Konteineren kan da startes på følgende måte:
```
$ USER=jenkins
$ docker run -d \
  -p 80:8080 \
  -v $SSH_AUTH_SOCK:/ssh_auth_sock \
  -v $(eval echo ~$USER)/.ssh/known_hosts:/var/jenkins_home/.ssh/known_hosts \
  -v $(eval echo ~$USER)/.docker:/var/jenkins_home/.docker \
  -v $(eval echo ~$USER)/.m2:/var/jenkins_home/.m2 \
  -e uid=`id -u $USER` \
  -e gid=`id -g $USER` \
  -e DOCKER_HOST=tcp://$HOSTNAME:2376 \
  --restart=unless-stopped \
  --name jenkins \
  docker-registry.dmz.local/eid-jenkins
```

## Hvordan vedlikeholde bildet

### Oppgradere Jenkins-versjon

Dockerfile inneholder følgende linjer:

```
ARG JENKINS_VERSION=2.23
ARG JENKINS_SHA=6c47f8f6019b9a2be17662033444ce7eec03f4fa
```

På http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/[versjon]/jenkins-war-[versjon].war finner du siste versjon, og SHA1-sum for denne finner du i http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/[versjon]/jenkins-war-[versjon].war.sha1. Erstatt argumentene over med disse for å bygge bilder med denne versjonen.

### Oppgradere tillegg

Det kjøres en rekke `RUN install-plugin.sh <navn> <versjon>` for de tilleggene som er i bruk. Her kan versjoner oppdateres, og nye tillegg kan også eventuelt legges til.

### Bygge bildet

```
$ docker build -t docker-registry.dmz.local/eid-jenkins .
```

### Publisere bildet

```
$ docker push docker-registry.dmz.local/eid-jenkins
```
