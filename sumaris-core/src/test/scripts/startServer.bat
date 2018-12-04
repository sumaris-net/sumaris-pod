@echo off

SET M2_REPO=%HOMEPATH%%\.m2\repository\

SET DB_DIRECTORY=../db-server
SET DB_OPTS=--database.0 file:%DB_DIRECTORY%/sumaris --dbname.0 sumaris

rem -- DB server AND a temp DB:
java -classpath %M2_REPO%\org\hsqldb\hsqldb\2.4.1\hsqldb-2.4.1.jar org.hsqldb.Server %DB_OPTS%
