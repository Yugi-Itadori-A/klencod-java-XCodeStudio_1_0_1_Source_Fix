@echo off
setlocal
set GRADLE_VERSION=8.11.1
set DIST_NAME=gradle-%GRADLE_VERSION%-bin
if defined GRADLE_USER_HOME (set BASE_DIR=%GRADLE_USER_HOME%\xcode-studio) else (set BASE_DIR=%USERPROFILE%\.gradle\xcode-studio)
set DIST_DIR=%BASE_DIR%\%DIST_NAME%
set GRADLE_BIN=%DIST_DIR%\gradle-%GRADLE_VERSION%\bin\gradle.bat
set ZIP_FILE=%BASE_DIR%\%DIST_NAME%.zip

if not exist "%GRADLE_BIN%" (
  if not exist "%BASE_DIR%" mkdir "%BASE_DIR%"
  echo XCode Studio: downloading Gradle %GRADLE_VERSION%...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/%DIST_NAME%.zip' -OutFile '%ZIP_FILE%'"
  if errorlevel 1 exit /b 1
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force '%ZIP_FILE%' '%BASE_DIR%'"
  if errorlevel 1 exit /b 1
  del "%ZIP_FILE%"
)

call "%GRADLE_BIN%" %*
exit /b %ERRORLEVEL%
