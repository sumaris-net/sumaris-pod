#!/bin/bash

export HSQLDB_VERSION=2.4.1
export HOME=`eval echo "~${USER}"`
export M2_REPO="$HOME/.m2/repository"
export CLASSPATH="${M2_REPO}/org/hsqldb/hsqldb/${HSQLDB_VERSION}/hsqldb-${HSQLDB_VERSION}.jar"
export DB_NAME="sumaris"
export TEST_DB="../../../target/db"
export DB_DIRECTORY="../../../target/db-server"
export JAVA_OPTS="-server -Xmx2g -Duser.timezone=Europe/Paris"

ARG=$1

# Make sure test DB exists
if [[ ! -f "${TEST_DB}/${DB_NAME}.script" ]]; then
    echo "Test DB not exists. Please run InitTest first !"

    exit
fi;

# Copy test DB
if [[ "${ARG}" == "--force" ]] || [[ ! -f "${DB_DIRECTORY}/${DB_NAME}.data" ]]; then
    echo "Copy test DB into 'target/db-server'"
    rm -rf ${DB_DIRECTORY}
    cp -R ${TEST_DB} ${DB_DIRECTORY}
    # Change 'readonly' value to false
    sed -i 's:^[ \t]*readonly[ \t]*=\([ \t]*.*\)$:readonly=false:' "${DB_DIRECTORY}/${DB_NAME}.properties"
fi;

cp ${CLASSPATH} .

export DB_OPTS="--database.0 file:${DB_DIRECTORY}/${DB_NAME} --dbname.0 ${DB_NAME}"

#export DB_TEMP_DIRECTORY="../db-temp"
#export DB_OPTS=$DB_OPTS --database.1 file:$DB_TEMP_DIRECTORY/${DB_NAME} --dbname.1 ${DB_NAME}-temp

# run db-server
java ${JAVA_OPTS} -classpath ${CLASSPATH} org.hsqldb.Server ${DB_OPTS}
