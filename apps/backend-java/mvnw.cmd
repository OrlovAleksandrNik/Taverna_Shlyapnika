@echo off
setlocal

set "BASE_DIR=%~dp0"
set "MAVEN_VERSION=3.9.9"
set "MAVEN_DIR=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%"
set "MAVEN_BIN=%MAVEN_DIR%\bin\mvn.cmd"
set "DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"
set "DIST_ZIP=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%-bin.zip"
set "POWERSHELL=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"

if exist "%MAVEN_BIN%" goto run_maven

where mvn >nul 2>nul
if %ERRORLEVEL%==0 (
  mvn %*
  exit /b %ERRORLEVEL%
)

if not exist "%BASE_DIR%.mvn" mkdir "%BASE_DIR%.mvn"

if not exist "%DIST_ZIP%" (
  "%POWERSHELL%" -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%DIST_ZIP%'"
  if not %ERRORLEVEL%==0 exit /b %ERRORLEVEL%
)

"%POWERSHELL%" -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%DIST_ZIP%' -DestinationPath '%BASE_DIR%.mvn' -Force"
if not %ERRORLEVEL%==0 exit /b %ERRORLEVEL%

:run_maven
"%MAVEN_BIN%" %*
exit /b %ERRORLEVEL%
