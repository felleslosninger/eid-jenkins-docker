FROM openjdk:8u92-jdk-alpine

ARG JENKINS_VERSION=2.23
ARG JENKINS_SHA=6c47f8f6019b9a2be17662033444ce7eec03f4fa
ARG MAVEN_VERSION=3.3.9
ARG DOCKER_VERSION=1.12.1
ARG DOCKER_SHA=05ceec7fd937e1416e5dce12b0b6e1c655907d349d52574319a1e875077ccb79
ARG user=jenkins
ARG group=jenkins
ENV uid 1000
ENV gid 1000

RUN apk add --no-cache git openssh-client curl zip unzip bash ttf-dejavu ca-certificates openssl groff py-pip

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

# Install AWS CLI
RUN pip install awscli && apk --purge -v del py-pip

ENV PATH=/usr/local/maven/bin:$PATH
ENV DOCKER_HOST tcp://localhost:2376
ENV DOCKER_TLS_VERIFY 1
ENV DOCKER_CERT_PATH $JENKINS_HOME/.docker
ENV SSH_AUTH_SOCK /ssh_auth_sock

COPY jenkins_home /var/jenkins_home
COPY init.sh /usr/local/bin/
COPY install-plugin.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/init.sh /usr/local/bin/install-plugin.sh

RUN install-plugin.sh ace-editor 1.1
RUN install-plugin.sh ant 1.4
RUN install-plugin.sh antisamy-markup-formatter 1.5
RUN install-plugin.sh branch-api 1.11
RUN install-plugin.sh build-timeout 1.17.1
RUN install-plugin.sh cloudbees-folder 5.13
RUN install-plugin.sh credentials-binding 1.9
RUN install-plugin.sh credentials 2.1.5
RUN install-plugin.sh display-url-api 0.5
RUN install-plugin.sh durable-task 1.12
RUN install-plugin.sh email-ext 2.51
RUN install-plugin.sh external-monitor-job 1.6
RUN install-plugin.sh git-client 2.0.0
RUN install-plugin.sh git-server 1.7
RUN install-plugin.sh git 3.0.0
RUN install-plugin.sh handlebars 1.1.1
RUN install-plugin.sh icon-shim 2.0.3
RUN install-plugin.sh javadoc 1.4
RUN install-plugin.sh jquery-detached 1.2.1
RUN install-plugin.sh junit 1.18
RUN install-plugin.sh mailer 1.18
RUN install-plugin.sh mapdb-api 1.0.9.0
RUN install-plugin.sh matrix-auth 1.4
RUN install-plugin.sh matrix-project 1.7.1
RUN install-plugin.sh momentjs 1.1.1
RUN install-plugin.sh pipeline-build-step 2.3
RUN install-plugin.sh pipeline-graph-analysis 1.1
RUN install-plugin.sh pipeline-input-step 2.1
RUN install-plugin.sh pipeline-milestone-step 1.0
RUN install-plugin.sh pipeline-rest-api 2.0
RUN install-plugin.sh pipeline-stage-step 2.2
RUN install-plugin.sh pipeline-stage-view 2.0
RUN install-plugin.sh plain-credentials 1.3
RUN install-plugin.sh scm-api 1.3
RUN install-plugin.sh script-security 1.23
RUN install-plugin.sh ssh-credentials 1.12
RUN install-plugin.sh ssh-slaves 1.11
RUN install-plugin.sh structs 1.5
RUN install-plugin.sh timestamper 1.8.6
RUN install-plugin.sh token-macro 2.0
RUN install-plugin.sh windows-slaves 1.2
RUN install-plugin.sh workflow-aggregator 2.4
RUN install-plugin.sh workflow-api 2.4
RUN install-plugin.sh workflow-basic-steps 2.2
RUN install-plugin.sh workflow-cps-global-lib 2.3
RUN install-plugin.sh workflow-cps 2.18
RUN install-plugin.sh workflow-durable-task-step 2.5
RUN install-plugin.sh workflow-job 2.7
RUN install-plugin.sh workflow-multibranch 2.9
RUN install-plugin.sh workflow-scm-step 2.2
RUN install-plugin.sh workflow-step-api 2.4
RUN install-plugin.sh workflow-support 2.8
RUN install-plugin.sh ws-cleanup 0.30

ENTRYPOINT init.sh && su jenkins -c "java -jar /usr/share/jenkins/jenkins.war --webroot=/tmp/jenkins/war --pluginroot=/tmp/jenkins/plugins"
