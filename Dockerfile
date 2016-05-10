FROM jenkins:2.0

ENV MAVEN_VERSION 3.3.9
COPY plugins.txt .
ADD jenkins_home /usr/share/jenkins/ref
USER root
RUN /usr/local/bin/plugins.sh plugins.txt && \
    chown -R jenkins.jenkins /usr/share/jenkins/ref && \
    cd /usr/local && \
    wget -O - http://mirrors.ibiblio.org/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xvzf - && \
    ln -sv /usr/local/apache-maven-$MAVEN_VERSION /usr/local/maven
USER ${user}
ENV PATH=/usr/local/maven/bin:$PATH
ENTRYPOINT sed -i "s/GIT_URL/$GIT_URL/g" /usr/share/jenkins/ref/jobs/build-from-jenkinsfile/config.xml && tini -- /usr/local/bin/jenkins.sh
