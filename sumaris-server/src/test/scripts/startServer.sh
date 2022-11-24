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
HOME=`eval echo "~$USER"`
APP_BASEDIR="${PROJECT_ROOT}/.local"
LOG_DIR="${APP_BASEDIR}/log/sumaris-pod.log"
DB_URL="jdbc:hsqldb:hsql://localhost/sumaris"
CONFIG_DIR="${PROJECT_ROOT}/.local/config"
ARGS=$*
PROFILE=hsqldb $ARGS

echo "Project root: ${PROJECT_ROOT}"
mkdir -p ${APP_BASEDIR}


# ------------------------------------
echo "${LOG_PREFIX} Building [core-shared], [core] and [server]... ${LOG_PREFIX}"
# ------------------------------------
cd ${PROJECT_ROOT}
#mvn install -pl sumaris-core-shared,sumaris-core,sumaris-server $MVN_INSTALL_OPTS
mvn install -pl sumaris-core-shared,sumaris-core,sumaris-extraction,sumaris-importation,sumaris-rdf,sumaris-server $MVN_INSTALL_OPTS
[[ $? -ne 0 ]] && exit 1

cd ${PROJECT_DIR}
VERSION=`grep -m1 -P "\<version>[0-9A−Z.]+(-\w*)?</version>" pom.xml | grep -oP "\d+.\d+.\d+(-\w*)?"`
WAR_FILE="${PROJECT_DIR}/target/sumaris-server-${VERSION}.war"

JAVA_OPTS=""
JAVA_OPTS="$JAVA_OPTS -Xms512m -Xmx1024m"
JAVA_OPTS="$JAVA_OPTS -Dspring.main.banner-mode=off"
JAVA_OPTS="$JAVA_OPTS -Dsumaris.basedir=${APP_BASEDIR}"
JAVA_OPTS="$JAVA_OPTS -Dsumaris.log.file=${LOG_DIR}"
JAVA_OPTS="$JAVA_OPTS -Dspring.datasource.url=${DB_URL}"
#JAVA_OPTS="$JAVA_OPTS -Drdf.enabled=true"
if [[ -d "${CONFIG_DIR}" ]]; then
  JAVA_OPTS="$JAVA_OPTS -Dspring.config.location=${CONFIG_DIR}/"
fi;
[[ "_${PROFILE}" != "_" ]]  && JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=${PROFILE}"

JAVA_CMD="java ${JAVA_OPTS} -jar ${WAR_FILE} ${ARGS}"

# ------------------------------------
echo "${LOG_PREFIX} Running pod from '${WAR_FILE}'... ${LOG_PREFIX}"
echo "Executing command: ${JAVA_CMD}"
echo "${LOG_PREFIX}"
# ------------------------------------
${JAVA_CMD}
