FROM openjdk:8u111-jdk-alpine

ARG JENKINS_VERSION=2.77
ARG JENKINS_SHA=a16df07f53091253460993794cb2d47d965a8155
ARG MAVEN_VERSION=3.5.0
ARG DOCKER_VERSION=17.06.1-ce
ARG DOCKER_SHA=e35fe12806eadbb7eb8aa63e3dfb531bda5f901cd2c14ac9cdcd54df6caed697
ARG DOCKER_MACHINE_VERSION=0.12.2
ENV uid 1000
ENV gid 1000

RUN apk add --no-cache coreutils git openssh-client curl zip unzip bash ttf-dejavu ca-certificates openssl groff py-pip python jq

ENV JENKINS_HOME /var/jenkins_home

RUN mkdir -p /usr/share/jenkins/

# Install Jenkins
RUN curl -fsSL http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/${JENKINS_VERSION}/jenkins-war-${JENKINS_VERSION}.war -o /usr/share/jenkins/jenkins.war \
  && echo "$JENKINS_SHA  /usr/share/jenkins/jenkins.war" | sha1sum -c -

# Install Maven
RUN cd /usr/local && \
    wget -O - http://mirrors.ibiblio.org/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xvzf - && \
    ln -sv /usr/local/apache-maven-$MAVEN_VERSION /usr/local/maven

# Install Docker (client)

RUN curl -fSL "https://download.docker.com/linux/static/stable/$(uname -m)/docker-$DOCKER_VERSION.tgz" -o docker.tgz \
	&& echo "${DOCKER_SHA} *docker.tgz" | sha256sum -c - \
	&& tar -xzvf docker.tgz \
	&& mv docker/* /usr/local/bin/ \
	&& rmdir docker \
	&& rm docker.tgz \
	&& docker -v

# Install Docker Machine
RUN curl -fSL https://github.com/docker/machine/releases/download/v${DOCKER_MACHINE_VERSION}/docker-machine-$(uname -s)-$(uname -m) >/usr/local/bin/docker-machine \
    && chmod +x /usr/local/bin/docker-machine

# Install AWS CLI
RUN pip install awscli && apk --purge -v del py-pip

ENV PATH=/usr/local/maven/bin:$PATH
ENV DOCKER_HOST tcp://localhost:2376
ENV DOCKER_TLS_VERIFY 1
ENV DOCKER_CERT_PATH $JENKINS_HOME/.docker
ENV JIRA_SITE jira

COPY files/ files/
RUN chmod +x /files/init.sh

EXPOSE 8080

ENTRYPOINT /files/init.sh && su $(id -nu ${uid}) -c "java -jar /usr/share/jenkins/jenkins.war --webroot=/tmp/jenkins/war --pluginroot=/tmp/jenkins/plugins"
