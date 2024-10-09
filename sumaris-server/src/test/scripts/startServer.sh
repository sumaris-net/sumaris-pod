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
#DB_URL="jdbc:hsqldb:file:${PROJECT_ROOT}/sumaris-core/target/db-server/sumaris"
CONFIG_DIR="${PROJECT_ROOT}/.local/config"
PROFILE="hsqldb,$1"
JVM_MAX_MEMORY=1024m

echo "Project root: ${PROJECT_ROOT}"
mkdir -p ${APP_BASEDIR}

# ------------------------------------
# BUILD
# ------------------------------------
if [[ "$@" == *"--build"* ]]; then
  echo "${LOG_PREFIX} Building [core-shared], [core] and [server]... ${LOG_PREFIX}"

  cd ${PROJECT_DIR} || exit 1
  rm -f target/sumaris-server-*.war

  cd ${PROJECT_ROOT} || exit 1

  #mvn install -pl sumaris-core-shared,sumaris-core,sumaris-server $MVN_INSTALL_OPTS
  mvn install -pl sumaris-core-shared,sumaris-core,sumaris-extraction,sumaris-importation,sumaris-server $MVN_INSTALL_OPTS
  [[ $? -ne 0 ]] && exit 1
fi

cd ${PROJECT_DIR} || exit 1
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
WAR_FILE="target/sumaris-server-*.war"

JAVA_OPTS=""
JAVA_OPTS="$JAVA_OPTS --enable-preview"
JAVA_OPTS="$JAVA_OPTS -Xms${JVM_MAX_MEMORY} -Xmx${JVM_MAX_MEMORY}"
JAVA_OPTS="$JAVA_OPTS -Dspring.main.banner-mode=off"
JAVA_OPTS="$JAVA_OPTS -Dsumaris.basedir=${APP_BASEDIR}"
JAVA_OPTS="$JAVA_OPTS -Dsumaris.log.file=${LOG_DIR}"
JAVA_OPTS="$JAVA_OPTS -Dspring.datasource.url=${DB_URL}"
#JAVA_OPTS="$JAVA_OPTS -Dspring.mail.enabled=false"
#JAVA_OPTS="$JAVA_OPTS -Drdf.enabled=true"
if [[ -d "${CONFIG_DIR}" ]]; then
  echo "Local config: $CONFIG_DIR"
  JAVA_OPTS="$JAVA_OPTS -Dspring.config.additional-location=${CONFIG_DIR}/"
fi;
if [[ "_${PROFILE}" != "_" ]]; then
  JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=${PROFILE}"
fi;

JAVA_CMD="java ${JAVA_OPTS} -jar ${WAR_FILE}"

# Mac OS - force arch
if [[ "$OSTYPE" == "darwin"* ]]; then
  JAVA_CMD="arch -x86_64  ${JAVA_CMD}"
fi;

# ------------------------------------
echo "${LOG_PREFIX} Running pod from '${WAR_FILE}'... ${LOG_PREFIX}"
echo "Executing command: ${JAVA_CMD}"
echo "${LOG_PREFIX}"
# ------------------------------------
${JAVA_CMD}
