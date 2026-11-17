@echo off
rem Builds dist\TintShadePaletteGenerator.jar  (needs a JDK 17+ on the PATH)
cd /d "%~dp0"
if exist build rmdir /s /q build
mkdir build\classes
if not exist dist mkdir dist
dir /s /b src\*.java > build\sources.txt
javac --release 17 -encoding UTF-8 -d build\classes @build\sources.txt || exit /b 1
xcopy /e /i /y resources build\classes >nul
jar --create --file dist\TintShadePaletteGenerator.jar --main-class com.jsoftbharath.palette.Main -C build\classes . || exit /b 1
echo Built dist\TintShadePaletteGenerator.jar
