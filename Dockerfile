FROM jenkins:1.642.4

USER jenkins
WORKDIR /tmp/files
COPY plugins.txt /tmp/files/
RUN /usr/local/bin/plugins.sh /tmp/files/plugins.txt

EXPOSE 8080 8081 9418
