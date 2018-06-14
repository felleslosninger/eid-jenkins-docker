FROM groovy:2.4.12-jre8-alpine AS groovy
FROM openjdk:8u131-jdk-alpine

ARG DOCKER_VERSION=17.09.0-ce
ARG DOCKER_SHA=a9e90a73c3cdfbf238f148e1ec0eaff5eb181f92f35bdd938fd7dab18e1c4647

RUN apk add --no-cache coreutils git openssh-client curl zip unzip bash ttf-dejavu ca-certificates openssl groff py-pip python jq


# Install Docker (client)
RUN curl -fSL "https://download.docker.com/linux/static/stable/$(uname -m)/docker-$DOCKER_VERSION.tgz" -o docker.tgz \
	&& echo "${DOCKER_SHA} *docker.tgz" | sha256sum -c - \
	&& tar -xzvf docker.tgz \
	&& mv docker/* /usr/local/bin/ \
	&& rmdir docker \
	&& rm docker.tgz \
	&& docker -v

# Install Groovy
ENV GROOVY_HOME /opt/groovy
COPY --from=groovy $GROOVY_HOME $GROOVY_HOME

ENV PATH=$GROOVY_HOME/bin:$PATH

RUN grape install 'org.yaml' 'snakeyaml' '1.19'

COPY files/ /files/
COPY entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]