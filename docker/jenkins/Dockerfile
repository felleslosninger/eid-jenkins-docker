ARG PLUGINS_IMAGE
FROM ${PLUGINS_IMAGE} AS plugins
FROM groovy:2.4.12-jre8-alpine AS groovy
FROM openjdk:8u131-jdk-alpine

ARG JENKINS_WAR_URL=http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war
ARG JENKINS_VERSION=2.148
ARG JENKINS_SHA=e2ef7e6d371e388e4199f901fff40b6e425c297fee8aa1c251284c62ef9bfc97

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

COPY static/ /static/
COPY scripts/ /scripts/
COPY templates/ /templates/

COPY --from=plugins /plugins /plugins
RUN grape install 'org.yaml' 'snakeyaml' '1.19'

EXPOSE 8080
HEALTHCHECK --interval=10s --start-period=10s --retries=60 \
    CMD curl -f -sS http://localhost:8080 || exit 1

COPY entrypoint.sh /
ENTRYPOINT ["/entrypoint.sh"]
