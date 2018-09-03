@echo off

SET M2_REPO=%HOMEPATH%%\.m2\repository\
set CLASSPATH=%M2_REPO%\org\hsqldb\hsqldb\2.3.2\hsqldb-2.3.2.jar
set CLASSPATH=%CLASSPATH%;%M2_REPO%\org\hsqldb\sqltool\2.3.2\sqltool-2.3.2.jar

rem install SqlTool 2.3.2
call mvn -q org.apache.maven.plugins:maven-dependency-plugin:2.1:get -DrepoUrl=http://download.java.net/maven/2/ -Dartifact=org.hsqldb:sqltool:2.3.2

rem Shutdown db-server
java -classpath %CLASSPATH% org.hsqldb.cmdline.SqlTool --rcFile=sqltool.rc --sql="shutdown;" quadrige3

rem Shutdown db-server-prod
rem java -classpath %CLASSPATH% org.hsqldb.cmdline.SqlTool --rcFile=sqltool.rc --sql="shutdown;" quadrige3-prod
