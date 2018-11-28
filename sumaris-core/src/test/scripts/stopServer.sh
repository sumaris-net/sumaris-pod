#!/bin/sh

export M2_REPO=$HOME/.m2/repository
export CLASSPATH=$M2_REPO/org/hsqldb/hsqldb/2.4.0/hsqldb-2.4.1.jar
export CLASSPATH=$CLASSPATH:$M2_REPO/org/hsqldb/sqltool/2.4.1/sqltool-2.4.1.jar

# install SqlTool 2.4.1
mvn -q org.apache.maven.plugins:maven-dependency-plugin:2.1:get -DrepoUrl=http://download.java.net/maven/2/ -Dartifact=org.hsqldb:sqltool:2.4.1

# Shutdown db-server
java -classpath $CLASSPATH org.hsqldb.cmdline.SqlTool --rcFile=sqltool.rc --sql="shutdown;" sumaris

# Shutdown db-server-prod
#java -classpath $CLASSPATH org.hsqldb.cmdline.SqlTool --rcFile=sqltool.rc --sql="shutdown;" quadrige3-prod
