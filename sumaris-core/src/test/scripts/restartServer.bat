@echo off

rem Init variables
set SCRIPT_DIR=%cd%
set PROJECT_DIR=..\..\..
cd %PROJECT_DIR%
set PROJECT_DIR=%cd%

echo [93mInstalling [core-shared] and [test-shared]...[0m
cd %PROJECT_DIR%\..
call mvn install -DskipTests -pl sumaris-core-shared,sumaris-test-shared --quiet
if not errorlevel 0 goto error

echo [93mGenerating new test DB... (log at: %PROJECT_DIR%\target\build.log)[0m
cd %PROJECT_DIR%
rmdir /S /Q target\db 2>nul
call mvn -Prun,hsqldb -DskipTests
if not errorlevel 0 goto error

echo [93mStopping DB server...[0m
cd %SCRIPT_DIR%
call stopServer.bat

echo [93mCleaning old DB server files...[0m
rmdir /S /Q %PROJECT_DIR%\target\db-server

echo [93mStarting DB server...[0m
cd %SCRIPT_DIR%
start "SUMARiS HSQLDB SERVER" "startServer.bat"

echo [92mOK[0m

goto end

:error
echo [91mSomething bad happened[0m

:end
cd %SCRIPT_DIR%