#!/bin/bash

# Get to the root project
if [[ "_" == "_${PROJECT_DIR}" ]]; then
  SCRIPT_DIR=$(dirname $0)
  PROJECT_DIR=$(cd "${SCRIPT_DIR}/../../.." && pwd)
  export PROJECT_DIR
fi;

# ------------------------------------
# Init variables
LOG_PREFIX="--------------"
#MVN_INSTALL_OPTS="-DskipTests --quiet --offline"
MVN_INSTALL_OPTS="-DskipTests --quiet"
PROJECT_ROOT=$(cd ${PROJECT_DIR}/.. && pwd)
APP_BASEDIR="${PROJECT_ROOT}/.local"
LOG_DIR="${APP_BASEDIR}/log/sumaris-pod.log"
DB_URL="jdbc:hsqldb:hsql://localhost/sumaris"
#CONFIG_DIR="${PROJECT_ROOT}/.local/config/"
ARGS=$*
PROFILE=hsqldb

echo "Project root: ${PROJECT_ROOT}"

# ------------------------------------
echo "${LOG_PREFIX} Installing [core-shared], [core] and [server]... ${LOG_PREFIX}"
# ------------------------------------
cd ${PROJECT_ROOT}
mvn install -pl sumaris-core-shared,sumaris-core,sumaris-server $MVN_INSTALL_OPTS
[[ $? -ne 0 ]] && exit 1

cd ${PROJECT_DIR}
VERSION=`grep -m1 -P "\<version>[0-9A−Z.]+(-\w*)?</version>" pom.xml | grep -oP "\d+.\d+.\d+(-\w*)?"`
WAR_FILE="${PROJECT_DIR}/target/sumaris-server-${VERSION}.war"

JAVA_OPTS=""
JAVA_OPTS="$JAVA_OPTS -Xms512m -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -Dsumaris.basedir=${APP_BASEDIR}"
#JAVA_OPTS="$JAVA_OPTS -Dsumaris.data.directory=${DATA_DIRECTORY}"
JAVA_OPTS="$JAVA_OPTS -Dsumaris.log.file=${LOG_DIR}"
JAVA_OPTS="$JAVA_OPTS -Dspring.datasource.url=${DB_URL}"
if [[ -d "${CONFIG_DIR}" ]]; then
  JAVA_OPTS="$JAVA_OPTS -Dspring.config.location=${PROJECT_ROOT}/.local/config/"
fi;
[[ "_${PROFILE}" != "_" ]]  && JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=${PROFILE}"

JAVA_CMD="java ${JAVA_OPTS} -jar ${WAR_FILE} ${ARGS}"

# ------------------------------------
echo "${LOG_PREFIX} Running ${WAR_FILE}... ${LOG_PREFIX}"
# ------------------------------------
mkdir -p ${APP_BASEDIR}
${JAVA_CMD}