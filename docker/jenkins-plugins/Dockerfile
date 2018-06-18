FROM maven:3.5.3-jdk-10-slim

COPY pom.xml /pom.xml
RUN mvn dependency:list # Initialize Maven in separate (cache) layer
COPY get-plugins /usr/local/bin/
COPY plugins.txt /plugins.txt
RUN get-plugins
