ARG IMAGE_REGISTRY
ARG DOCKER_IMAGE="openjdk:17-jdk-slim"
#FROM $IMAGE_REGISTRY$DOCKER_IMAGE
FROM $DOCKER_IMAGE

# Build arg
ARG ENTRYPOINT=app.sh
ARG WAR_FILE=app.war
ARG CONFIG_FILES=config/*.properties
ARG USERNAME="spring"
ARG USERID="20001"
ARG GROUPNAME="spring"
ARG GROUPID="20001"

# Install dependencies
RUN apt-get update \
    && apt-get install -y --no-install-recommends libsodium-dev

# Create execution user/group
RUN echo "Creating group ${GROUPNAME}:${GROUPID}" && \
    groupadd -g ${GROUPID} ${GROUPNAME} && \
    echo "Creating user ${USERNAME}:${USERID}" && \
    useradd -g ${GROUPNAME} -d /app -u ${USERID} ${USERNAME}

USER ${USERNAME}:${GROUPNAME}

# Copy files
COPY --chown=${USERNAME}:${GROUPNAME} ${ENTRYPOINT} /app/app.sh
RUN chmod +x /app/app.sh && \
    mkdir -p /app/logs && chown ${USERNAME}:${GROUPNAME} /app/logs && \
    mkdir -p /app/data && chown ${USERNAME}:${GROUPNAME} /app/data \

COPY --chown=${USERNAME}:${GROUPNAME} ${WAR_FILE} /app/app.war
COPY --chown=${USERNAME}:${GROUPNAME} ${CONFIG_FILES} /app/config/

# Run
ENV PORT=8080 \
    TZ=UTC \
    PROFILES=default
EXPOSE ${PORT}
ENTRYPOINT ["/app/app.sh"]