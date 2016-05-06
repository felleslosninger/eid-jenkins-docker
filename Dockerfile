FROM jenkins:2.0

USER root

ENV MAVEN_VERSION 3.3.9
RUN cd /usr/local; wget -O - http://mirrors.ibiblio.org/apache/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | tar xvzf -
RUN ln -sv /usr/local/apache-maven-$MAVEN_VERSION /usr/local/maven

USER jenkins

COPY plugins.txt .
RUN /usr/local/bin/plugins.sh plugins.txt

ADD jenkins_home /usr/share/jenkins/ref
USER root
RUN chown -R jenkins.jenkins /usr/share/jenkins/ref

USER jenkins
ENV PATH=/usr/local/maven/bin:$PATH

EXPOSE 8080
