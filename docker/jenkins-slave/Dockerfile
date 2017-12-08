FROM openjdk:8u131-jdk-alpine

ARG MAVEN_VERSION=3.5.2
ARG DOCKER_VERSION=17.09.0-ce
ARG DOCKER_SHA=a9e90a73c3cdfbf238f148e1ec0eaff5eb181f92f35bdd938fd7dab18e1c4647

RUN apk add --no-cache coreutils git openssh-client curl zip unzip bash ttf-dejavu ca-certificates openssl groff py-pip python jq

# Install Maven
RUN cd /usr/local && \
    curl -fSL "http://mirrors.ibiblio.org/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz" | tar xvzf - && \
    ln -sv /usr/local/apache-maven-$MAVEN_VERSION /usr/local/maven

# Install Docker (client)
RUN curl -fSL "https://download.docker.com/linux/static/stable/$(uname -m)/docker-$DOCKER_VERSION.tgz" -o docker.tgz \
	&& echo "${DOCKER_SHA} *docker.tgz" | sha256sum -c - \
	&& tar -xzvf docker.tgz \
	&& mv docker/* /usr/local/bin/ \
	&& rmdir docker \
	&& rm docker.tgz \
	&& docker -v

ENV PATH=/usr/local/maven/bin:$GROOVY_HOME/bin:$PATH

COPY entrypoint.sh /entrypoint.sh

ENTRYPOINT /entrypoint.sh