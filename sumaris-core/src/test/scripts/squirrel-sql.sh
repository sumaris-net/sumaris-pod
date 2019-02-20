#!/bin/bash

export JAVA_HOME=/usr/lib/jvm/java-8-oracle/

if [[ "_${SQUIRREL_HOME}" == "_" ]]; then
    export SQUIRREL_HOME=/opt/squirrel-sql
    echo "No env variable SQUIRREL_HOME defined. Will use: ${SQUIRREL_HOME}"
fi;

DIRNAME=`pwd`
HSQLDB_JAR=`ls | grep hsqldb*.jar`
if [[ "_${HSQLDB_JAR}" != "_" ]]; then
    HSQLDB_JAR_PATH="${DIRNAME}/${HSQLDB_JAR}"
else

    echo "No HsqlDB jar file found in script dir"
    HSQLDB_VERSION=`ls ${HOME}/.m2/repository/org/hsqldb/hsqldb/ | grep 2 | sort | tail -1`
    if [[ "_${HSQLDB_VERSION}" == "_" ]]; then
        HSQLDB_VERSION="2.4.1" # Default version
        echo "Using default HsqlDB version: ${HSQLDB_VERSION}"
    fi;

    HSQLDB_JAR="hsqldb-${HSQLDB_VERSION}.jar"
    HSQLDB_JAR_PATH="${HOME}/.m2/repository/org/hsqldb/hsqldb/${HSQLDB_VERSION}/${HSQLDB_JAR}"

    if [[ ! -f ${HSQLDB_JAR_PATH} ]]; then
        echo "ERROR: Unable to found HsqlDB jar file, neither in script directory, neither in ~/.m2/repository"
        exit 1
    fi;

    echo "Will use HsqlDB jar file found at ${HSQLDB_JAR_PATH}"
fi;

# Make sure Squirrel is installed
if [[ ! -d ${SQUIRREL_HOME} ]]; then
    echo "ERROR: Squirrel home not exists at ${SQUIRREL_HOME}"
    exit 1
fi

# Installing jar into Squirrel
if [[ ! -f "${SQUIRREL_HOME}/lib/${HSQLDB_JAR}" ]]; then

    cd "${SQUIRREL_HOME}/lib"

    HSQLDB_OLD_JAR=`ls | grep hsqldb*.jar`
    if [[ "_${HSQLDB_OLD_JAR}" != "_" ]]; then
        echo "Removing old jar file: ${SQUIRREL_HOME}/lib/${HSQLDB_OLD_JAR}"
        sudo rm ${HSQLDB_OLD_JAR}
        if [[ $? -ne 0 ]]; then
            exit 1
        fi
    fi

    echo "Installing ${HSQLDB_JAR} to ${SQUIRREL_HOME}/lib ..."
    sudo cp ${HSQLDB_JAR_PATH} ${SQUIRREL_HOME}/lib/
    if [[ $? -ne 0 ]]; then
        exit 1
    fi
fi;

echo "Opening Squirrel SQL..."
cd ${SQUIRREL_HOME}
./squirrel-sql.sh &
