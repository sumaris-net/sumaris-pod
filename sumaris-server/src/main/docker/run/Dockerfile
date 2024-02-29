ARG IMAGE_REGISTRY
ARG DOCKER_IMAGE="eclipse-temurin:17-jdk-alpine"
FROM $IMAGE_REGISTRY$DOCKER_IMAGE

# Build arg
ARG ENTRYPOINT=app.sh
ARG WAR_FILE=app.war
ARG CONFIG_FILES=config/*.properties
ARG USERNAME="spring"
ARG USERID="20001"
ARG GROUPNAME="spring"
ARG GROUPID="20001"

# Create execution user/group
RUN \
    echo "Creation group ${GROUPNAME}:${GROUPID}" && \
    addgroup -g ${GROUPID} ${GROUPNAME} && \
    echo "Creating user ${USERNAME}:${USERID}" && \
    adduser -u ${USERID} -G ${GROUPNAME} -h /app -D ${USERNAME}
USER ${USERNAME}:${GROUPNAME}

# Copy files
COPY --chown=${USERNAME}:${GROUPNAME} ${ENTRYPOINT} /app/app.sh
RUN chmod +x /app/app.sh
RUN mkdir -p /app/logs && chown ${USERNAME}:${GROUPNAME} /app/logs
RUN mkdir -p /app/data && chown ${USERNAME}:${GROUPNAME} /app/data
COPY --chown=${USERNAME}:${GROUPNAME} ${WAR_FILE} /app/app.war
COPY --chown=${USERNAME}:${GROUPNAME} ${CONFIG_FILES} /app/config/

# Run
ENV PORT=8080 \
    TZ=UTC \
    PROFILES=default
EXPOSE ${PORT}
ENTRYPOINT ["/app/app.sh"]