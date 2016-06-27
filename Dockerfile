FROM java:openjdk-8-jdk-alpine

ARG JENKINS_VERSION=2.10
ARG JENKINS_SHA=e5238e09c8a6dbd96daeb0f1bf2baf20501def28
ARG MAVEN_VERSION=3.3.9
ARG user=jenkins
ARG group=jenkins
ENV uid 1000
ENV gid 1000

RUN apk add --no-cache git openssh-client curl zip unzip bash ttf-dejavu

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
COPY plugins.txt .
RUN plugins.sh plugins.txt

VOLUME /maven-repo

RUN cd /usr/local && \
    wget -O - http://mirrors.ibiblio.org/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xvzf - && \
    ln -sv /usr/local/apache-maven-$MAVEN_VERSION /usr/local/maven

ENV PATH=/usr/local/maven/bin:$PATH
ENV DOCKER_HOST tcp://eid-jenkins01.dmz.local:2376
ENV DOCKER_TLS_VERIFY 1

ENTRYPOINT init.sh && su jenkins -c "java -jar /usr/share/jenkins/jenkins.war --webroot=/tmp/jenkins/war --pluginroot=/tmp/jenkins/plugins"
