ARG PLUGINS_IMAGE
FROM ${PLUGINS_IMAGE} AS plugins
FROM groovy:2.4.12-jre8-alpine AS groovy
FROM openjdk:8u131-jdk-alpine

ARG JENKINS_WAR_URL=http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war
ARG JENKINS_VERSION=2.102
ARG JENKINS_SHA=df57d15616cc18fbf20a43d8dae8ccdec9a6881e514c56f8530a47a7187c1e35
ENV uid 1000
ENV gid 1000

RUN apk add --no-cache coreutils git openssh-client curl zip unzip bash ttf-dejavu ca-certificates openssl groff py-pip python jq

ENV JENKINS_HOME /var/jenkins_home

RUN mkdir -p /usr/share/jenkins/

# Install Jenkins
RUN curl -fsSL ${JENKINS_WAR_URL}/${JENKINS_VERSION}/jenkins-war-${JENKINS_VERSION}.war -o /usr/share/jenkins/jenkins.war \
  && echo "$JENKINS_SHA  /usr/share/jenkins/jenkins.war" | sha256sum -c -

# Install Groovy
ENV GROOVY_HOME /opt/groovy
COPY --from=groovy $GROOVY_HOME $GROOVY_HOME

ENV PATH=$GROOVY_HOME/bin:$PATH

COPY files/ /files/
RUN chmod +x /files/init.sh

COPY --from=plugins /files/plugins /plugins

EXPOSE 8080
HEALTHCHECK CMD curl -f -sS http://localhost:8080 || exit 1

ENTRYPOINT /files/init.sh && su $(id -nu ${uid}) -c "java -jar /usr/share/jenkins/jenkins.war --webroot=/tmp/jenkins/war --pluginroot=/tmp/jenkins/plugins"
