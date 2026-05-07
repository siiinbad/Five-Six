@echo off
setlocal
REM Build then run main.Main using bin/ as the classpath root.

call "%~dp0build.cmd" || exit /b 1

set "ROOT=%~dp0"
set "CP=%ROOT%bin;%ROOT%lib\*"

set "JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA=%JAVA_HOME%\bin\java.exe"

"%JAVA%" --enable-preview -XX:+ShowCodeDetailsInExceptionMessages -cp "%CP%" main.Main
