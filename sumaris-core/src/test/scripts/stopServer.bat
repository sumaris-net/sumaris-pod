@echo off

SET HSQLDB_VERSION=2.4.1
SET M2_REPO=%HOMEDRIVE%%HOMEPATH%\.m2\repository\
set CLASSPATH=%M2_REPO%\org\hsqldb\hsqldb\%HSQLDB_VERSION%\hsqldb-%HSQLDB_VERSION%.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\hsqldb\sqltool\%HSQLDB_VERSION%\sqltool-%HSQLDB_VERSION%.jar

rem install SqlTool %HSQLDB_VERSION%
call mvn -q org.apache.maven.plugins:maven-dependency-plugin:2.1:get -DrepoUrl=http://download.java.net/maven/2/ -Dartifact=org.hsqldb:sqltool:%HSQLDB_VERSION%

rem Shutdown db-server
java -classpath %CLASSPATH% org.hsqldb.cmdline.SqlTool --rcFile=sqltool.rc --sql="shutdown;" sumaris

