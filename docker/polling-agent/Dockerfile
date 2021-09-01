FROM openjdk:11.0.11-jdk-slim

RUN groupadd -g 1001 polling-agent
RUN useradd -rm  -s /bin/bash -g polling-agent -u 1001 polling-agent
RUN mkdir /opt/polling-agent; chown polling-agent:polling-agent /opt/polling-agent
RUN mkdir /data; chown polling-agent:polling-agent /data
COPY --chown=polling-agent:polling-agent polling-agent.jar /opt/polling-agent/polling-agent.jar
COPY --chown=polling-agent:polling-agent entrypoint.sh /entrypoint.sh
USER polling-agent
WORKDIR /opt/polling-agent/
ENTRYPOINT ["/entrypoint.sh"]