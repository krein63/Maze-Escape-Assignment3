@echo off
cd /d "%~dp0"
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 goto error
java -cp out Main
if errorlevel 1 goto error
start "" "output\maze.html"
pause
exit /b 0

:error
echo Check that Java JDK 17 or newer is installed and available in PATH.
pause
exit /b 1
