ARG IMAGE_REGISTRY
ARG DOCKER_IMAGE="maven:3.9.6-eclipse-temurin-17-alpine"
FROM $IMAGE_REGISTRY$DOCKER_IMAGE
ARG MAVEN_LOCAL_REPO="/root/.m2/repository/"
WORKDIR /tmp/.build-cache
COPY ./ ./
RUN mkdir -p $MAVEN_LOCAL_REPO && \
    du -s `find $MAVEN_LOCAL_REPO -maxdepth 3 | egrep -v "^\.$"` > /tmp/before.txt

# Caching maven dependencies
RUN mvn --batch-mode install -q -DskipTests && \
    mvn --batch-mode clean -q && \
    du -s `find $MAVEN_LOCAL_REPO -maxdepth 3 | egrep -v "^\.$"` > /tmp/after.txt && \
    diff /tmp/before.txt /tmp/after.txt || true

# install system required libraries
RUN apk update && \
    apk add git && \
    apk add zip

ENV MAVEN_LOCAL_REPO=$MAVEN_LOCAL_REPO

WORKDIR /build
