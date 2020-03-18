FROM groovy:2.5-jre8-alpine AS groovy
FROM docker:19.03.6 as docker
FROM openjdk:8u131-jdk-alpine

RUN apk add --no-cache bash openssh git curl jq
ENV GROOVY_HOME /opt/groovy
COPY --from=groovy $GROOVY_HOME $GROOVY_HOME
ENV PATH=$GROOVY_HOME/bin:$PATH
COPY --from=docker /usr/local/bin/docker /usr/bin/docker
RUN grape install 'org.apache.httpcomponents' 'httpclient' '4.5.11'

COPY *.groovy /
COPY project /tmp/project
COPY entrypoint.sh /

ENTRYPOINT /entrypoint.sh
