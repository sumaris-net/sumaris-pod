#!/bin/bash
BASEDIR=/app
JAVA_OPTS="-Dsumaris.basedir=${BASEDIR}"
JAVA_OPTS="$JAVA_OPTS -Dspring.config.location=${BASEDIR}/config/"
JAVA_OPTS="$JAVA_OPTS -Doracle.net.tns_admin=/home/tnsnames"
JAVA_OPTS="$JAVA_OPTS -Doracle.jdbc.timezoneAsRegion=false"
[[ "_${PROFILES}" != "_" ]] && JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=${PROFILES}"
[[ "_${TZ}" != "_" ]] && JAVA_OPTS="$JAVA_OPTS -Duser.timezone=${TZ}"
[[ "_${PORT}" -ne "_" ]] && JAVA_OPTS="$JAVA_OPTS -Dserver.port=${PORT}"
ARGS=${@:2}
echo "Starting, with arguments: ${ARGS} - profiles: ${PROFILES}"
java ${JAVA_OPTS} -server -jar /app/app.war ${ARGS}