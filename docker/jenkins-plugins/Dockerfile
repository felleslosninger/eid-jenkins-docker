FROM maven:3.8.1-jdk-11

COPY pom.xml /pom.xml
RUN mvn dependency:list # Initialize Maven in separate (cache) layer
COPY get-plugins /usr/local/bin/
COPY plugins.txt /plugins.txt
RUN get-plugins
