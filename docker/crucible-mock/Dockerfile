FROM adoptopenjdk/openjdk11:jdk-11.0.11_9-alpine

ENV WIREMOCK_VERSION 2.15.0

RUN wget https://repo1.maven.org/maven2/com/github/tomakehurst/wiremock-standalone/$WIREMOCK_VERSION/wiremock-standalone-$WIREMOCK_VERSION.jar \
    -O /wiremock-standalone.jar

COPY /mappings /mappings/

EXPOSE 80

ENTRYPOINT java -jar /wiremock-standalone.jar --port 80
