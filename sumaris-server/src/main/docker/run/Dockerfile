FROM openjdk:11-jdk

# Build arg
ARG ENTRYPOINT=app.sh
ARG WAR_FILE=app.war
ARG CONFIG_FILES=config/*.properties
ARG USERNAME="spring"
ARG USERID="20001"
ARG GROUPNAME="spring"
ARG GROUPID="20001"

# Install dependencies
RUN apt update && apt -y install libsodium-dev

# Create execution user/group
RUN echo "Creating group ${GROUPNAME}:${GROUPID}"
RUN groupadd -g ${GROUPID} ${GROUPNAME}
RUN echo "Creating user ${USERNAME}:${USERID}"
RUN useradd -g ${GROUPNAME} -d /app -u ${USERID} ${USERNAME}
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