@echo off
setlocal
REM Build Java sources into bin/ (keeps existing bin/res assets intact).

set "ROOT=%~dp0"
set "CP=%ROOT%lib\*"

set "JAVAC=javac"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"

set "SOURCES="
for %%F in ("%ROOT%src\entity\*.java") do call set "SOURCES=%%SOURCES%% "%%~fF""
for %%F in ("%ROOT%src\main\*.java") do call set "SOURCES=%%SOURCES%% "%%~fF""

"%JAVAC%" --release 17 -cp "%CP%" -d "%ROOT%bin" %SOURCES%
if errorlevel 1 exit /b 1

echo Build OK. Output: %ROOT%bin
