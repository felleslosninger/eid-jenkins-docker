FROM groovy:3.0.8-jre11 AS groovy
FROM adoptopenjdk/openjdk11:jdk-11.0.11_9-alpine


ARG DOCKER_VERSION=18.06.3-ce
ARG DOCKER_SHA=346f9394393ee8db5f8bd1e229ee9d90e5b36931bdd754308b2ae68884dd6822
# https://hub.docker.com/_/docker?tab=tags&page=1&name=-ce

RUN apk add --no-cache coreutils git openssh-client curl zip unzip bash ttf-dejavu ca-certificates openssl groff jq
# Install Groovy
ENV GROOVY_HOME /opt/groovy
COPY --from=groovy $GROOVY_HOME $GROOVY_HOME

# Install Docker (client)
RUN curl -fSL "https://download.docker.com/linux/static/stable/$(uname -m)/docker-$DOCKER_VERSION.tgz" -o docker.tgz \
	&& echo "${DOCKER_SHA} *docker.tgz" | sha256sum -c - \
	&& tar -xzvf docker.tgz \
	&& mv docker/* /usr/local/bin/ \
	&& rmdir docker \
	&& rm docker.tgz \
	&& docker -v

# Install Groovy
ENV GROOVY_HOME /opt/groovy
COPY --from=groovy $GROOVY_HOME $GROOVY_HOME

ENV PATH=$GROOVY_HOME/bin:$PATH

RUN grape install 'org.yaml' 'snakeyaml' '1.19'

RUN addgroup -S jenkins -g 1001  && adduser -S jenkins -G jenkins -G ping  -u 1001
RUN chown jenkins:jenkins /etc/ssh/
# Tell docker that all future commands should run as the appuser user
USER jenkins
COPY files/ /files/
COPY entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]