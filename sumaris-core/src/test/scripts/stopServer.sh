#!/bin/bash

SCRIPT_DIR=$(dirname $0)
SCRIPT_DIR=$(cd "${SCRIPT_DIR}" && pwd)

# Get to the project directory
if [[ "_" == "_${PROJECT_DIR}" ]]; then
  PROJECT_DIR=$(cd "${SCRIPT_DIR}/../../.." && pwd)
  export PROJECT_DIR
fi;

export HSQLDB_VERSION=2.4.1
export HOME=`eval echo "~$USER"`
export M2_REPO="$HOME/.m2/repository"
export CLASSPATH="$M2_REPO/org/hsqldb/hsqldb/$HSQLDB_VERSION/hsqldb-$HSQLDB_VERSION.jar"
export CLASSPATH="$CLASSPATH:$M2_REPO/org/hsqldb/sqltool/$HSQLDB_VERSION/sqltool-$HSQLDB_VERSION.jar"

# install SqlTool $HSQLDB_VERSION
mvn -q org.apache.maven.plugins:maven-dependency-plugin:2.1:get -DrepoUrl=http://download.java.net/maven/2/ -Dartifact=org.hsqldb:sqltool:$HSQLDB_VERSION

# Shutdown db-server
echo "Stopping DB at ${SCRIPT_DIR}"
cd ${SCRIPT_DIR}
java -classpath $CLASSPATH org.hsqldb.cmdline.SqlTool --rcFile=sqltool.rc --sql="shutdown;" sumaris

# Shutdown db-server-prod
#java -classpath $CLASSPATH org.hsqldb.cmdline.SqlTool --rcFile=sqltool.rc --sql="shutdown;" quadrige3-prod
