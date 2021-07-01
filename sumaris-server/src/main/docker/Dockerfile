FROM openjdk:8-jdk

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
RUN echo "Creatin group ${GROUPNAME}:${GROUPID}"
RUN groupadd -g ${GROUPID} ${GROUPNAME}
RUN echo "Creatin user ${USERNAME}:${USERID}"
RUN useradd -g ${GROUPNAME} -d /app -u ${USERID} ${USERNAME}
USER ${USERNAME}:${GROUPNAME}

# Copy files
RUN ls -artl
COPY ${ENTRYPOINT} /app/app.sh
RUN ls -artl /app
RUN chmod +x /app/app.sh
RUN mkdir -p /app/logs
RUN mkdir -p /app/data
COPY ${WAR_FILE} /app/app.war
COPY ${CONFIG_FILES} /app/config/

# Run
ENV PORT=8080 \
    TZ=UTC \
    PROFILES=default
EXPOSE ${PORT}
ENTRYPOINT ["/app/app.sh"]