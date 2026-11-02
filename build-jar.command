#!/bin/bash
set -euo pipefail
cd -- "$(dirname -- "$0")"
command -v javac >/dev/null || { echo 'Install JDK 17 or newer first'; exit 1; }
mkdir -p build/jar-classes dist
find src/main/java -name '*.java' > build/jar-sources.txt
javac --release 17 -encoding UTF-8 -d build/jar-classes @build/jar-sources.txt
cp -R src/main/resources/. build/jar-classes/
jar --create --file dist/BorrowedEnergy.jar --main-class borrowedenergy.Main -C build/jar-classes .
echo 'Created dist/BorrowedEnergy.jar'
