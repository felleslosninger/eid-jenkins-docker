ARG PLUGINS_IMAGE
FROM ${PLUGINS_IMAGE} AS plugins
FROM groovy:3.0.8-jre11 AS groovy
FROM adoptopenjdk/openjdk11:jdk-11.0.11_9-alpine

ARG JENKINS_WAR_URL=https://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war
ARG JENKINS_VERSION=2.298
ARG JENKINS_SHA=448bce07af6aa287892ea5abda5d0a4cdcff7b2255918a3accd00322077b5162

RUN apk add --no-cache coreutils git openssh-client curl zip unzip bash ttf-dejavu ca-certificates openssl groff jq
RUN addgroup -g 1001 -S jenkins && adduser -u 1001 -S jenkins -G jenkins
ENV JENKINS_HOME /var/jenkins_home

RUN mkdir -p /usr/share/jenkins/
RUN mkdir -p /var/jenkins_home/; chown jenkins:jenkins /var/jenkins_home/
RUN mkdir -p /builds; chown jenkins:jenkins /builds/
RUN mkdir -p /workspaces/; chown jenkins:jenkins /workspaces/
RUN chown -R jenkins:jenkins /etc/ssh/

# Install Jenkins
RUN curl -fsSL ${JENKINS_WAR_URL}/${JENKINS_VERSION}/jenkins-war-${JENKINS_VERSION}.war -o /usr/share/jenkins/jenkins.war \
  && echo "$JENKINS_SHA  /usr/share/jenkins/jenkins.war" | sha256sum -c -

# Install Groovy
ENV GROOVY_HOME /opt/groovy
COPY --from=groovy $GROOVY_HOME $GROOVY_HOME

ENV PATH=$GROOVY_HOME/bin:$PATH
RUN grape install 'org.yaml' 'snakeyaml' '1.19'

COPY --chown=jenkins:jenkins static/ /static/
COPY --chown=jenkins:jenkins scripts/ /scripts/
COPY --chown=jenkins:jenkins templates/ /templates/
COPY --chown=jenkins:jenkins --from=plugins /plugins /plugins



# Tell docker that all future commands should run as the appuser user
USER jenkins

EXPOSE 8080
HEALTHCHECK --interval=10s --start-period=10s --retries=60 \
    CMD curl -f -sS http://localhost:8080/login || exit 1
COPY entrypoint.sh /
ENTRYPOINT ["/entrypoint.sh"]
