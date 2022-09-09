# PREPARE STAGE
ARG IMAGE_REGISTRY
ARG DOCKER_IMAGE="maven:3.8.4-openjdk-11-slim"
FROM $IMAGE_REGISTRY$DOCKER_IMAGE AS prepare
ARG MAVEN_LOCAL_REPO="/root/.m2/repository/"
WORKDIR /tmp/.build-cache
COPY ./ ./
# set all modules to version 0.0.0 in order not to upload dependencies if only version change
RUN mvn versions:set -DnewVersion=0.0.0 -q

# BUILD STAGE
FROM $IMAGE_REGISTRY$DOCKER_IMAGE AS build
WORKDIR /build
# Caching maven depedencies
COPY --from=prepare /tmp/.build-cache /build
RUN mvn dependency:go-offline -q
# install system required libraries
RUN apt update && \
    apt -y install git