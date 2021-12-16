#!/bin/bash

APP_NAME=sumaris
BASEDIR=/app
LOG_DIR=/app/logs
JAVA_OPTS="-D${APP_NAME}.basedir=${BASEDIR}"
JAVA_OPTS="${JAVA_OPTS} -Dspring.config.location=file:${BASEDIR}/config/"
JAVA_OPTS="${JAVA_OPTS} -Dsumaris.log.file=${LOG_DIR}/${APP_NAME}-pod.log"
JAVA_OPTS="${JAVA_OPTS} -Doracle.net.tns_admin=/home/tnsnames"
JAVA_OPTS="${JAVA_OPTS} -Doracle.jdbc.timezoneAsRegion=false"
[[ "_${PROFILES}" != "_" ]] && JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=${PROFILES}"
[[ "_${TZ}" != "_" ]] && JAVA_OPTS="${JAVA_OPTS} -Duser.timezone=${TZ}"
[[ "_${PORT}" != "_" ]] && JAVA_OPTS="${JAVA_OPTS} -Dserver.port=${PORT}"
ARGS=
# TODO test this
#ARGS=${@:2}

echo "*** Starting ${APP_NAME}-pod - args: ${ARGS} - profiles: ${PROFILES} ***"

java ${JAVA_OPTS} -server -jar ${BASEDIR}/app.war ${ARGS}