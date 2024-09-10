#!/bin/bash

# Get to the root project
if [[ "_" == "_${PROJECT_DIR}" ]]; then
  SCRIPT_DIR=$(dirname $0)
  PROJECT_DIR=$(cd "${SCRIPT_DIR}/../../.." && pwd)
  export PROJECT_DIR
fi;

export HSQLDB_VERSION=2.4.1
export HOME=$(eval echo "~${USER}")
export M2_REPO="$HOME/.m2/repository"
export CLASSPATH="${M2_REPO}/org/hsqldb/hsqldb/${HSQLDB_VERSION}/hsqldb-${HSQLDB_VERSION}.jar"
export DB_NAME="sumaris"
export TEST_DB="$PROJECT_DIR/target/db"
export DB_PORT=9001
export DB_DIRECTORY="$PROJECT_DIR/target/db-server"
export DB_TIMEZONE=UTC
#export DB_TIMEZONE=Europe/Paris
#export JAVA_OPTS="-server -Xmx2g -Duser.timezone=${DB_TIMEZONE}"
export JAVA_OPTS="-server -Xms256m -Xmx512m -Duser.timezone=${DB_TIMEZONE}"

ARG=$1

# Make sure test DB exists
if [[ ! -f "${TEST_DB}/${DB_NAME}.script" ]]; then
    echo "Test DB not exists. Please run InitTest first !"
    exit 1
fi;

# Copy test DB
if [[ "${ARG}" == "--force" ]] || [[ ! -f "${DB_DIRECTORY}/${DB_NAME}.data" ]]; then
    echo "Copy test DB into 'target/db-server'"
    rm -rf ${DB_DIRECTORY}
    cp -R ${TEST_DB} ${DB_DIRECTORY} || exit 1

    # Change 'readonly' value to false
    sed 's/^[ \t]*readonly[ \t]*=\([ \t]*.*\)$/readonly=false/i' ${TEST_DB}/${DB_NAME}.properties > ${DB_DIRECTORY}/${DB_NAME}.properties
    [[ $? -ne 0 ]] && exit 1
fi;

cp ${CLASSPATH} .

DB_OPTS="--database.0 file:${DB_DIRECTORY}/${DB_NAME} --dbname.0 ${DB_NAME} --port ${DB_PORT}"

JAVA_CMD="java ${JAVA_OPTS} -classpath ${CLASSPATH} org.hsqldb.Server ${DB_OPTS}"

#export DB_TEMP_DIRECTORY="../db-temp"
#export DB_OPTS=$DB_OPTS --database.1 file:$DB_TEMP_DIRECTORY/${DB_NAME} --dbname.1 ${DB_NAME}-temp

if [[ ! -d "${DB_DIRECTORY}" ]]; then
    echo "Cannot found the DB directory: ${DB_DIRECTORY}"
fi;

# run db-server
echo ${JAVA_CMD}
${JAVA_CMD}
