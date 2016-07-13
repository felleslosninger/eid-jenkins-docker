FROM java:openjdk-8-jdk-alpine

ARG JENKINS_VERSION=2.13
ARG JENKINS_SHA=3eb1a8e9bf396a56595b75449ba245012287f4dd
ARG MAVEN_VERSION=3.3.9
ARG DOCKER_VERSION=1.11.2
ARG DOCKER_SHA=8c2e0c35e3cda11706f54b2d46c2521a6e9026a7b13c7d4b8ae1f3a706fc55e1
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

ADD jenkins_home /var/jenkins_home
COPY init.sh /usr/local/bin/
COPY plugins.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/{init,plugins}.sh
COPY plugins.txt .
RUN plugins.sh plugins.txt

VOLUME /maven-repo

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

ENTRYPOINT init.sh && su jenkins -c "java -jar /usr/share/jenkins/jenkins.war --webroot=/tmp/jenkins/war --pluginroot=/tmp/jenkins/plugins"
