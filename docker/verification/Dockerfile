FROM groovy:2.4.12-jre8-alpine AS groovy
FROM docker:17.12.1-ce as docker
FROM openjdk:8u131-jdk-alpine

RUN apk add --no-cache bash openssh git curl jq
ENV GROOVY_HOME /opt/groovy
COPY --from=groovy $GROOVY_HOME $GROOVY_HOME
ENV PATH=$GROOVY_HOME/bin:$PATH
COPY --from=docker /usr/local/bin/docker /usr/bin/docker
COPY *.groovy /
COPY entrypoint.sh /
RUN grape install 'org.apache.httpcomponents' 'httpclient' '4.5.5'

ENTRYPOINT /entrypoint.sh
