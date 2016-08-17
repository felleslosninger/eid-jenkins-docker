# Jenkins i Docker-konteiner

Dette er en prekonfigurert Jenkins-instans som benytter pipeline-as-code-konseptet.

## Hvordan kjøre konteineren

Her forutsettes det at verten har en bruker (`jenkins`) med en nøkkel for å aksessere Docker-motoren på https://eid-jenkins01.dmz.local:2376. Brukeren har også en SHH-nøkkel for å aksessere Git-verten til eid-prosjektet, og den har et lokalt Maven-depot.

```
$ USER=jenkins
$ docker run -d \
  -p 80:8080 \
  -v ~$USER/.docker/key.pem:/tmp/docker_key \
  -v ~$USER/.ssh/id_rsa:/tmp/git_key \
  -v ~$USER/.ssh/key_saml_metadata_validator:/tmp/key_saml_metadata_validator \
  -v ~$USER/.m2/repository:/maven-repo \
  -e uid=`id -u $USER` \
  -e gid=`id -g $USER` \
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

På http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/[versjon]/jenkins-war-[versjon].war finner du siste versjon, og SHA1-sum for denne finner du i http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/[versjon]/jenkins-war-[versjon].war.sha1. Erstatt argumentene over med disse for å bygge bilder med denne versjonen.

### Oppgradere tillegg

Fila `plugins.txt` inneholder en liste over de tilleggene som er i bruk. Her kan versjoner oppdateres, og nye tillegg kan også eventuelt legges til.

### Bygge bildet

```
$ docker build -t docker-registry.dmz.local/eid-jenkins .
```

### Publisere bildet

```
$ docker push docker-registry.dmz.local/eid-jenkins
```
