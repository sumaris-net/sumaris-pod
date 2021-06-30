FROM openjdk:8-jdk
ARG ENTRYPOINT=app.sh
ARG WAR_FILE=app.war
ARG CONFIG_FILES=config/*.properties
RUN apt update && apt -y install libsodium-dev
# container user managment
COPY ${ENTRYPOINT} /app/app.sh
RUN chmod +x /app/app.sh
COPY ${WAR_FILE} /app/app.war
COPY ${CONFIG_FILES} /app/config/
ENV USERNAME="sih_web" \
    GROUPNAME="ditiisi" \
    GROUPID="10004"
# TODO : remove when logfile will be overwrite
RUN touch sumaris.log.file_IS_UNDEFINED
RUN groupadd -g $GROUPID $GROUPNAME \
    && useradd -g $GROUPNAME $USERNAME \
    && chown -R $USERNAME:$GROUPNAME /app \
    && chown -R $USERNAME:$GROUPNAME sumaris.log.file_IS_UNDEFINED
USER $USERNAME
ENV PORT=8080 \
    TZ=UTC \
    PROFILES=default
EXPOSE ${PORT}
ENTRYPOINT ["/app/app.sh"]