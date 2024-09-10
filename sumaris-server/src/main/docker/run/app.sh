#!/bin/bash

#
# %L
# SUMARiS
# %%
# Copyright (C) 2019 SUMARiS Consortium
# %%
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public
# License along with this program.  If not, see
# <http://www.gnu.org/licenses/gpl-3.0.html>.
# L%
#

# Allow group to modify files on each newly created file.
# This is usefull when we run container with -group-add="${GROUPNAME}".
umask 002

[[ "_${APP_NAME}" == "_" ]] && APP_NAME=sumaris
[[ "_${BASEDIR}" == "_" ]] && BASEDIR=/app
[[ "_${LOG_DIR}" == "_" ]] && LOG_DIR=/app/logs
[[ "_${LOG_FILENAME}" == "_" ]] && LOG_FILENAME="${APP_NAME}-pod.log"
[[ "_${TNS_ADMIN}" == "_" ]] && TNS_ADMIN=/home/tnsnames
JAVA_OPTS="${JAVA_OPTS} --enable-preview" # Fix Java 17 error
JAVA_OPTS="${JAVA_OPTS} -Dsumaris.name=${APP_NAME}"
JAVA_OPTS="${JAVA_OPTS} -Dsumaris.basedir=${BASEDIR}"
JAVA_OPTS="${JAVA_OPTS} -Dsumaris.log.file=${LOG_DIR}/${LOG_FILENAME}"
JAVA_OPTS="${JAVA_OPTS} -Dspring.config.location=file:${BASEDIR}/config/"
JAVA_OPTS="${JAVA_OPTS} -Doracle.net.tns_admin=${TNS_ADMIN}"
JAVA_OPTS="${JAVA_OPTS} -Doracle.jdbc.timezoneAsRegion=false"
[[ "_${PROFILES}" != "_" ]] && JAVA_OPTS="${JAVA_OPTS} -Dspring.profiles.active=${PROFILES}"
[[ "_${TZ}" != "_" ]] && JAVA_OPTS="${JAVA_OPTS} -Duser.timezone=${TZ}"
[[ "_${PORT}" != "_" ]] && JAVA_OPTS="${JAVA_OPTS} -Dserver.port=${PORT}"
[[ "_${XMS}" != "_" ]] && JAVA_OPTS="${JAVA_OPTS} -Xms${XMS}"
[[ "_${XMX}" != "_" ]] && JAVA_OPTS="${JAVA_OPTS} -Xmx${XMX}"

ARGS=
# TODO test this
#ARGS=${@:2}

echo "*** Starting ${APP_NAME}-pod - args: ${ARGS} - profiles: ${PROFILES} ***"

#echo "java ${JAVA_OPTS} -server -jar ${BASEDIR}/app.war ${ARGS}"
java ${JAVA_OPTS} -server -jar ${BASEDIR}/app.war ${ARGS}
