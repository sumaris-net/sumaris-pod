ARG IMAGE_REGISTRY
ARG DOCKER_IMAGE="maven:3.8.5-eclipse-temurin-17-alpine"
FROM $IMAGE_REGISTRY$DOCKER_IMAGE
ARG MAVEN_LOCAL_REPO="/root/.m2/repository/"
WORKDIR /tmp/.build-cache
COPY ./ ./
RUN mkdir -p $MAVEN_LOCAL_REPO
RUN du -s `find $MAVEN_LOCAL_REPO -maxdepth 3 | egrep -v "^\.$"` > /tmp/before.txt
# Caching maven dependencies
RUN mvn install -q -DskipTests
RUN mvn clean -q
#RUN mvn dependency:go-offline -q
RUN du -s `find $MAVEN_LOCAL_REPO -maxdepth 3 | egrep -v "^\.$"` > /tmp/after.txt
RUN diff /tmp/before.txt /tmp/after.txt || true
# install system required libraries
RUN apk update && \
    apk add git && \
    apk add zip
ENV MAVEN_LOCAL_REPO=$MAVEN_LOCAL_REPO
WORKDIR /build
