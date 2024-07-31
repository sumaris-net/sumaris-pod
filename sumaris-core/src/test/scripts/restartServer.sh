#!/bin/bash
export JAVA_HOME="/Users/eis/Library/Java/JavaVirtualMachines/temurin-17.0.11/Contents/Home"
SCRIPT_DIR=$(dirname $0)
SCRIPT_DIR=$(cd "${SCRIPT_DIR}" && pwd)

# Get to the root project
if [[ "_" == "_${PROJECT_DIR}" ]]; then
  PROJECT_DIR=$(cd "${SCRIPT_DIR}/../../.." && pwd)
  export PROJECT_DIR
fi;

# ------------------------------------
# Init variables
# ------------------------------------

LOG_PREFIX="--------------"
export DB_TIMEZONE=UTC
#export DB_TIMEZONE=Europe/Paris
#MVN_ARGS="-DskipTests --quiet --offline"
MVN_ARGS="-DskipTests -Duser.timezone=${DB_TIMEZONE} --quiet"
PROJECT_ROOT=$(cd ${PROJECT_DIR}/.. && pwd)

# ------------------------------------
echo "${LOG_PREFIX} Installing [core-shared] and [test-shared]... ${LOG_PREFIX}"
# ------------------------------------
cd "${PROJECT_ROOT}"
mvn install -pl sumaris-core-shared,sumaris-test-shared $MVN_ARGS
[[ $? -ne 0 ]] && exit 1

# ------------------------------------
echo "${LOG_PREFIX} Generating new test DB... (log at: ${PROJECT_DIR}/target/build.log) ${LOG_PREFIX}"
# ------------------------------------
cd "${PROJECT_DIR}"
PROJECT_DIR=`pwd`
rm -rf target/db
mvn -Prun,hsqldb $MVN_ARGS
#mvn -Prun,hsqldb $MVN_ARGS | grep -P "(WARN|ERROR|FAILURE)"
[[ $? -ne 0 ]] && exit 1

# Make sure test DB exists
if [[ ! -f "target/db/sumaris.script" ]]; then
    echo "Test DB not exists. Please run InitTest first !"
    exit 1
fi;

# ------------------------------------
echo "${LOG_PREFIX} Stopping DB server...       ${LOG_PREFIX}"
# ------------------------------------
cd "${SCRIPT_DIR}"
./stopServer.sh

# ------------------------------------
echo "${LOG_PREFIX} Cleaning old DB server files... ${LOG_PREFIX}"
# ------------------------------------
rm -rf "${PROJECT_DIR}/target/db-server"
cd "${SCRIPT_DIR}"

# ------------------------------------
echo "${LOG_PREFIX} Starting DB server...       ${LOG_PREFIX}"
# ------------------------------------
nohup ./startServer.sh --force &

# ------------------------------------
echo "${LOG_PREFIX} Starting DB server [OK] (log at: ${SCRIPT_DIR}/nohup.out) ${LOG_PREFIX}"

