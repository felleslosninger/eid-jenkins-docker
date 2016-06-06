FROM java:openjdk-8-jdk-alpine

RUN apk add --no-cache git openssh-client curl zip unzip bash ttf-dejavu

ENV JENKINS_HOME /var/jenkins_home

# Jenkins home directory is a volume, so configuration and build history
# can be persisted and survive image upgrades
#VOLUME /var/jenkins_home

ENV JENKINS_VERSION 2.7
ENV JENKINS_SHA 69e3ab0cc44acc3d711efb7436505e967174d628

RUN mkdir -p /usr/share/jenkins/

# could use ADD but this one does not check Last-Modified header
# see https://github.com/docker/docker/issues/8331
RUN curl -fsSL http://repo.jenkins-ci.org/public/org/jenkins-ci/main/jenkins-war/${JENKINS_VERSION}/jenkins-war-${JENKINS_VERSION}.war -o /usr/share/jenkins/jenkins.war \
  && echo "$JENKINS_SHA  /usr/share/jenkins/jenkins.war" | sha1sum -c -

ENV JENKINS_UC https://updates.jenkins.io

# for main web interface:
EXPOSE 8080

ADD jenkins_home /var/jenkins_home
COPY plugins.sh /usr/local/bin/plugins.sh
COPY plugins.txt .
RUN /usr/local/bin/plugins.sh plugins.txt

ARG user=jenkins
ARG group=jenkins
ARG uid=1000
ARG gid=1000
RUN addgroup -g ${gid} ${group} && adduser -h "$JENKINS_HOME" -u ${uid} -G ${group} -s /bin/bash -D ${user}

RUN chown -R jenkins:jenkins $JENKINS_HOME

VOLUME /maven-repo

ENV MAVEN_VERSION 3.3.9
RUN cd /usr/local && \
    wget -O - http://mirrors.ibiblio.org/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xvzf - && \
    ln -sv /usr/local/apache-maven-$MAVEN_VERSION /usr/local/maven

ENV PATH=/usr/local/maven/bin:$PATH

ENTRYPOINT cp /tmp/git_key $JENKINS_HOME/git_key && chown jenkins $JENKINS_HOME/git_key && su jenkins -c "java -jar /usr/share/jenkins/jenkins.war --webroot=/tmp/jenkins/war --pluginroot=/tmp/jenkins/plugins"
