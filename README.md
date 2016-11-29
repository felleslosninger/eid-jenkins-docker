# Jenkins i Docker-konteiner

Dette er en prekonfigurert Jenkins-instans som benytter pipeline-as-code-konseptet.

## Tilgjengelige verktøy

Følgende verktøy er tilgjengelig for byggejobber direkte fra `PATH`:
* Java Development Kit 8 (`java`)
* Maven 3.3 (`mvn`)
* Docker klient 1.12 (`docker`)
* Docker Machine 0.8 (`docker-machine`)
* AWS CLI (`aws`)

## Hvordan kjøre applikasjonen

Følgende krav stilles til vertsmaskinen:
* Docker Engine (1.12.1 eller nyere) er installert.
* Det finnes en bruker `jenkins`.

For byggejobber som trenger Docker:
* En Docker Engine, enten den samme som Jenkins kjører i, eller en fjern, som er konfigurert for [toveis TLS-aksess](https://docs.docker.com/engine/security/https/).
* Brukeren `jenkins` har en `.docker`-katalog med nødvendige PEM-filer for å aksessere en Docker Engine via TLS.

For byggejobber som trenger SSH-aksess til tjenere:
* Brukeren `jenkins` har en fil `.ssh/known_hosts` med fingeravtrykkene til de tjenerne Jenkins vil kontakte med SSH.
* Miljøvariabelen `SSH_AUTH_SOCK` inneholder stien til en Unix-socket for en kjørende
  [ssh-agent](https://wiki.archlinux.org/index.php/SSH_keys#SSH_agents) som holder autentiseringsnøkler for de tjenerne
  Jenkins vil kontakte med SSH.
  
For byggejobber som trenger AWS:
* Brukeren `jenkins` har en `.aws`-katalog med nødvendig legitimasjon og innstillinger. Legitimasjonen må peke til
  en AWS-bruker som har nødvendige rettigheter til å utføre det byggejobbene trenger
  (f.eks. en IAM-bruker med sikkerhetspolicien `AmazonEC2FullAccess`).

Applikasjonen kan kjøres som en tjeneste på en Docker-sverm. `pipeline/application.sh create` gjør dette for deg.

Alternativt kan en enkelt konteiner startes på følgende måte:
```
$ USER=jenkins
$ docker run -d \
  -p 80:8080 \
  -v $SSH_AUTH_SOCK:/ssh_auth_sock \
  -v $(eval echo ~$USER)/data:/var/jenkins_home/ \
  -v $(eval echo ~$USER)/.ssh/known_hosts:/var/jenkins_home/.ssh/known_hosts \
  -v $(eval echo ~$USER)/.docker:/var/jenkins_home/.docker \
  -v $(eval echo ~$USER)/.aws:/var/jenkins_home/.aws \
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
