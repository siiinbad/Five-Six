@echo off
setlocal
REM Build Java sources into bin/ (keeps existing bin/res assets intact).

set "ROOT=%~dp0"
set "CP=%ROOT%lib\*"

set "JAVAC=javac"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"

"%JAVAC%" --enable-preview --release 25 -cp "%CP%" -d "%ROOT%bin" "%ROOT%src\entity\*.java" "%ROOT%src\main\*.java"
if errorlevel 1 exit /b 1

echo Build OK. Output: %ROOT%bin
