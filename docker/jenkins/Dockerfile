ARG PLUGINS_VERSION
FROM eid-jenkins02.dmz.local:8081/jenkins-plugins:${PLUGINS_VERSION} AS plugins
FROM groovy:2.4.12-jre8-alpine AS groovy
FROM openjdk:8u131-jdk-alpine

ARG JENKINS_WAR_URL=http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war
ARG JENKINS_VERSION=2.93
ARG JENKINS_SHA=bdd13054c2a96e8c35733cb6256d85da640fee77
ARG MAVEN_VERSION=3.5.2
ARG DOCKER_VERSION=17.09.0-ce
ARG DOCKER_SHA=a9e90a73c3cdfbf238f148e1ec0eaff5eb181f92f35bdd938fd7dab18e1c4647
ENV uid 1000
ENV gid 1000

RUN apk add --no-cache coreutils git openssh-client curl zip unzip bash ttf-dejavu ca-certificates openssl groff py-pip python jq

ENV JENKINS_HOME /var/jenkins_home

RUN mkdir -p /usr/share/jenkins/

# Install Jenkins
RUN curl -fsSL ${JENKINS_WAR_URL}/${JENKINS_VERSION}/jenkins-war-${JENKINS_VERSION}.war -o /usr/share/jenkins/jenkins.war \
  && echo "$JENKINS_SHA  /usr/share/jenkins/jenkins.war" | sha1sum -c -

# Install Maven
RUN cd /usr/local && \
    wget -O - http://apache.uib.no/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xvzf - && \
    ln -sv /usr/local/apache-maven-$MAVEN_VERSION /usr/local/maven

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

ENV PATH=/usr/local/maven/bin:$GROOVY_HOME/bin:$PATH
ENV DOCKER_TLS_VERIFY 1
ENV DOCKER_CERT_PATH /docker

COPY files/ /files/
RUN chmod +x /files/init.sh

COPY --from=plugins /files/plugins /plugins

EXPOSE 8080
HEALTHCHECK CMD curl -f -sS http://localhost:8080 || exit 1

ENTRYPOINT /files/init.sh && su $(id -nu ${uid}) -c "java -jar /usr/share/jenkins/jenkins.war --webroot=/tmp/jenkins/war --pluginroot=/tmp/jenkins/plugins"
