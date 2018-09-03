@echo off

SET M2_REPO=%HOMEPATH%%\.m2\repository\

SET DB_DIRECTORY=../db-server
SET DB_OPTS=--database.0 file:%DB_DIRECTORY%/sumaris --dbname.0 sumaris

SET DB_DIRECTORY_PROD=../db-server-prod
rem SET DB_OPTS=%DB_OPTS% --database.1 file:%DB_DIRECTORY_PROD%/sumaris --dbname.1 sumaris-prod

SET DB_TEMP_DIRECTORY=../db-temp
rem SET DB_OPTS=%DB_OPTS% --database.2 file:%DB_TEMP_DIRECTORY%/sumaris --dbname.2 sumaris-temp

rem -- DB server AND a temp DB:
java -classpath %M2_REPO%\org\hsqldb\hsqldb\2.4.0\hsqldb-2.4.0.jar org.hsqldb.Server %DB_OPTS%
