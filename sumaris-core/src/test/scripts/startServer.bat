@echo off

SET HSQLDB_VERSION=2.4.1
SET M2_REPO=%HOMEDRIVE%%HOMEPATH%\.m2\repository\
set CLASSPATH=%M2_REPO%\org\hsqldb\hsqldb\%HSQLDB_VERSION%\hsqldb-%HSQLDB_VERSION%.jar

set DB_NAME=sumaris
set TEST_DB=..\..\..\target\db
set DB_DIRECTORY=..\..\..\target\db-server
SET DB_OPTS=--database.0 file:%DB_DIRECTORY%/%DB_NAME% --dbname.0 %DB_NAME%
SET JAVA_OPTS=-server -Xmx2g -Duser.timezone=UTC

rem make sure test DB exists
if not exist "%TEST_DB%\%DB_NAME%.script" (
    echo [91mTest DB not exists. Please run InitTest first ![0m
    goto exit
)

rem Copy test DB
echo [93mCopy test DB into 'target\db-server'[0m
rmdir /S /Q %DB_DIRECTORY% 2>nul
xcopy /S %TEST_DB% %DB_DIRECTORY%\
rem Change 'readonly' value to false with PowerShell
PowerShell -Command "(Get-Content %DB_DIRECTORY%\%DB_NAME%.properties).replace('readonly=true', 'readonly=false') | Set-Content %DB_DIRECTORY%\%DB_NAME%.properties"

rem run DB server :
echo [93mStartup HSQLDB Server [4m%HSQLDB_VERSION%[0m
java %JAVA_OPTS% -classpath %CLASSPATH% org.hsqldb.Server %DB_OPTS%

rem exit this console here (eg. if the hsqldb is stopped)
exit

rem don't exit if error happened
:exit