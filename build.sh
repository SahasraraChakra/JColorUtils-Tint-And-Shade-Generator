#!/bin/sh
# Builds dist/TintShadePaletteGenerator.jar  (needs a JDK 17+ on the PATH)
set -e
cd "$(dirname "$0")"
rm -rf build && mkdir -p build/classes dist
javac --release 17 -encoding UTF-8 -d build/classes $(find src -name '*.java')
cp -r resources/* build/classes/
jar --create --file dist/TintShadePaletteGenerator.jar --main-class com.jsoftbharath.palette.Main -C build/classes .
echo "Built dist/TintShadePaletteGenerator.jar"
