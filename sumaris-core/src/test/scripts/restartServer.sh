#!/bin/bash

# ------------------------------------
# Init variables
SCRIPT_DIR=`pwd`
PROJECT_DIR="../../.."
cd "${PROJECT_DIR}"
PROJECT_DIR=`pwd`
LOG_PREFIX="--------------"
#MVN_INSTALL_OPTS="-DskipTests --quiet --offline"
MVN_INSTALL_OPTS="-DskipTests --quiet --offline"

# ------------------------------------
echo "${LOG_PREFIX} Installing [core-shared] and [test-shared]... ${LOG_PREFIX}"
# ------------------------------------
cd "${PROJECT_DIR}/.."
mvn install -pl sumaris-core-shared,sumaris-test-shared $MVN_INSTALL_OPTS
if [[ $? -ne 0 ]]; then
    exit
fi

# ------------------------------------
echo "${LOG_PREFIX} Generating new test DB... (log at: ${PROJECT_DIR}/target/build.log) ${LOG_PREFIX}"
# ------------------------------------
cd "${PROJECT_DIR}"
PROJECT_DIR=`pwd`
rm -rf target/db
mvn -Prun,hsqldb $MVN_INSTALL_OPTS
#mvn -Prun,hsqldb -DskipTests --quiet | grep -P "(WARN|ERROR|FAILURE)"
if [[ $? -ne 0 ]]; then
    exit
fi

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

