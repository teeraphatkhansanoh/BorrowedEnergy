#!/bin/bash
set -euo pipefail

cd -- "$(dirname -- "$0")"

if ! javac -version >/dev/null 2>&1; then
    printf '%s\n' 'ไม่พบ Java Compiler: ต้องใช้ JDK 17 หรือใหม่กว่า' >&2
    exit 1
fi

sources=()
while IFS= read -r -d '' source_file; do
    sources+=("$source_file")
done < <(find src/main/java src/test/java -type f -name '*.java' -print0)

mkdir -p build/test-classes
javac --release 17 -encoding UTF-8 -d build/test-classes "${sources[@]}"
if [ -d src/main/resources ]; then
    cp -R src/main/resources/. build/test-classes/
fi
java -ea -Djava.awt.headless=true -cp build/test-classes borrowedenergy.ModelTest
exec java -ea -Djava.awt.headless=true -cp build/test-classes borrowedenergy.UiInputTest
