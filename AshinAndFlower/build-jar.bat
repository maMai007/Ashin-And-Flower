@echo off
rem Build dist\AshinAndFlower.jar (code + res packed inside, runs on Java 17+)
setlocal
cd /d "%~dp0"
set BUILD=build

if exist "%BUILD%" rmdir /s /q "%BUILD%"
mkdir "%BUILD%"

echo Compiling...
javac --release 17 -encoding UTF-8 -d "%BUILD%" src\ashinandflower\*.java
if errorlevel 1 goto fail

echo Copying res...
xcopy res "%BUILD%" /e /i /q /y >nul
if errorlevel 1 goto fail

rem jar.exe is often not on PATH (Oracle installer adds only java/javac), so look in JAVA_HOME or java.home
set JAR=jar
where jar >nul 2>nul
if not errorlevel 1 goto pack
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set "JAR=%JAVA_HOME%\bin\jar.exe"& goto pack
for /f "tokens=1,* delims==" %%a in ('java -XshowSettings:properties -version 2^>^&1 ^| findstr /c:"java.home"') do set "JHOME=%%b"
if defined JHOME set "JAR=%JHOME:~1%\bin\jar.exe"

:pack
echo Packing jar...
if not exist dist mkdir dist
if exist dist\AshinAndFlower.jar del dist\AshinAndFlower.jar
"%JAR%" --create --file dist\AshinAndFlower.jar --main-class ashinandflower.Game -C "%BUILD%" .
if errorlevel 1 goto fail

echo.
echo Build OK: dist\AshinAndFlower.jar
goto end

:fail
echo.
echo Build FAILED

:end
pause
