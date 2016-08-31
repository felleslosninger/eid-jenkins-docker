FROM openjdk:8u92-jdk-alpine

ARG JENKINS_VERSION=2.20
ARG JENKINS_SHA=356796398a447f6cca6313bbf1dfaba24560698d
ARG MAVEN_VERSION=3.3.9
ARG DOCKER_VERSION=1.12.1
ARG DOCKER_SHA=05ceec7fd937e1416e5dce12b0b6e1c655907d349d52574319a1e875077ccb79
ARG user=jenkins
ARG group=jenkins
ENV uid 1000
ENV gid 1000

RUN apk add --no-cache git openssh-client curl zip unzip bash ttf-dejavu ca-certificates openssl

ENV JENKINS_HOME /var/jenkins_home

RUN mkdir -p /usr/share/jenkins/

# could use ADD but this one does not check Last-Modified header
# see https://github.com/docker/docker/issues/8331
RUN curl -fsSL http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/${JENKINS_VERSION}/jenkins-war-${JENKINS_VERSION}.war -o /usr/share/jenkins/jenkins.war \
  && echo "$JENKINS_SHA  /usr/share/jenkins/jenkins.war" | sha1sum -c -

ENV JENKINS_UC https://updates.jenkins.io

# for main web interface:
EXPOSE 8080

# Install Maven
RUN cd /usr/local && \
    wget -O - http://mirrors.ibiblio.org/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xvzf - && \
    ln -sv /usr/local/apache-maven-$MAVEN_VERSION /usr/local/maven

# Install Docker (client)
RUN set -x \
	&& curl -fSL "https://get.docker.com/builds/Linux/x86_64/docker-$DOCKER_VERSION.tgz" -o docker.tgz \
	&& echo "${DOCKER_SHA} *docker.tgz" | sha256sum -c - \
	&& tar -xzvf docker.tgz \
	&& mv docker/* /usr/local/bin/ \
	&& rmdir docker \
	&& rm docker.tgz \
	&& docker -v

ENV PATH=/usr/local/maven/bin:$PATH
ENV DOCKER_HOST tcp://eid-jenkins01.dmz.local:2376
ENV DOCKER_TLS_VERIFY 1
ENV DOCKER_CERT_PATH $JENKINS_HOME/.docker

COPY jenkins_home /var/jenkins_home
COPY init.sh /usr/local/bin/
COPY plugins.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/init.sh /usr/local/bin/plugins.sh
COPY plugins.txt .
RUN plugins.sh plugins.txt

ENTRYPOINT init.sh && su jenkins -c "java -jar /usr/share/jenkins/jenkins.war --webroot=/tmp/jenkins/war --pluginroot=/tmp/jenkins/plugins"
