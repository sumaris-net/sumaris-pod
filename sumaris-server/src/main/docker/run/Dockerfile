ARG IMAGE_REGISTRY
ARG DOCKER_IMAGE="openjdk:17-jdk-slim"
#FROM $IMAGE_REGISTRY$DOCKER_IMAGE
FROM $DOCKER_IMAGE

# Build arg
ARG ENTRYPOINT=app.sh
ARG WAR_FILE=app.war
ARG CONFIG_FILES=config/*.properties
ARG USERNAME="udocker"
ARG USERID="9999"
ARG GROUPNAME="gcontainer"
ARG GROUPID="9999"

# Install dependencies
RUN apt-get update \
    && apt-get install -y --no-install-recommends libsodium-dev

# Create execution user/group
RUN echo "Creating group ${GROUPNAME}:${GROUPID}" && \
    groupadd -g ${GROUPID} ${GROUPNAME} && \
    echo "Creating user ${USERNAME}:${USERID}" && \
    useradd -g ${GROUPNAME} -d /app -u ${USERID} ${USERNAME}

# Copy files
COPY --chown=${USERNAME}:${GROUPNAME} ${ENTRYPOINT} /app/app.sh
COPY --chown=${USERNAME}:${GROUPNAME} ${CONFIG_FILES} /app/config/

# Make executable and change ownership
RUN chmod +x /app/app.sh && \
    mkdir -p /app/logs /app/data && \
    chown -R ${USERNAME}:${GROUPNAME} /app/logs /app/data \

COPY --chown=${USERNAME}:${GROUPNAME} ${WAR_FILE} /app/app.war

USER ${USERNAME}:${GROUPNAME}

# Run
ENV PORT=8080 \
    TZ=UTC \
    PROFILES=default
EXPOSE ${PORT}
ENTRYPOINT ["/app/app.sh"]