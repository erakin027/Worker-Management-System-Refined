@echo off
setlocal

:: CONFIGURE
set JAR=lib\gson-2.13.2.jar
set SRC=src\app
set MAIN=app.Main

if "%1"=="" goto usage
if /I "%1"=="compile" goto compile
if /I "%1"=="run" goto run
if /I "%1"=="all" goto all
if /I "%1"=="clean" goto clean
goto usage

:compile
echo Compiling Java sources in %SRC%...
javac -cp "%JAR%" "%SRC%\*.java"
if errorlevel 1 (
  echo.
  echo *** Compilation failed.
  exit /b 1
)
echo Compilation succeeded.
exit /b 0

:run
echo Running %MAIN%...
java -cp "src;%JAR%" %MAIN%
exit /b 0

:all
call "%~f0" compile
if errorlevel 1 exit /b 1
call "%~f0" run
exit /b 0

:clean
echo Cleaning .class files in %SRC%...
del /q "%SRC%\*.class" 2>nul
echo Clean complete.
exit /b 0

:usage
echo Usage: compile.bat ^[compile^|run^|all^|clean^]
exit /b 1
