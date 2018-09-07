#!/bin/bash

export JAVA_HOME=/usr/lib/jvm/java-8-oracle/
export SQUIRREL_HOME=/opt/squirrel-sql-3.5.2

rm ${SQUIRREL_HOME}/lib/hsqldb-*.jar
cp hsqldb-*.jar ${SQUIRREL_HOME}/lib/
cd ${SQUIRREL_HOME}
./squirrel-sql.sh
