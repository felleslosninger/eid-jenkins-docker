ARG PLUGINS_IMAGE
FROM ${PLUGINS_IMAGE} AS plugins
FROM groovy:2.4.12-jre8-alpine AS groovy
FROM openjdk:8u131-jdk-alpine

ARG JENKINS_WAR_URL=http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war
ARG JENKINS_VERSION=2.108
ARG JENKINS_SHA=396406c34da9d7f7710a1d5d60057a67c2e97b6ff77e1516585b4b40d9c566b6

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
COPY scripts /scripts/
COPY templates /templates/
RUN chmod +x /files/init.sh

COPY --from=plugins /files/plugins /plugins
RUN grape install 'org.yaml' 'snakeyaml' '1.19'

EXPOSE 8080
HEALTHCHECK CMD curl -f -sS http://localhost:8080 || exit 1

ENTRYPOINT /files/init.sh && java -jar /usr/share/jenkins/jenkins.war --webroot=/tmp/jenkins/war --pluginroot=/tmp/jenkins/plugins
