#!/bin/bash
set -euo pipefail

cd -- "$(dirname -- "$0")"

if ! javac -version >/dev/null 2>&1; then
    printf '%s\n' 'ไม่พบ Java Compiler: โปรดใช้ JDK 17 หรือใหม่กว่า แล้วเปิดไฟล์นี้อีกครั้ง' >&2
    exit 1
fi

sources=()
while IFS= read -r -d '' source_file; do
    sources+=("$source_file")
done < <(find src/main/java -type f -name '*.java' -print0)

if [ "${#sources[@]}" -eq 0 ]; then
    printf '%s\n' 'ไม่พบซอร์สโค้ดเกมใน src/main/java' >&2
    exit 1
fi

mkdir -p build/classes
javac --release 17 -encoding UTF-8 -d build/classes "${sources[@]}"
if [ -d src/main/resources ]; then
    cp -R src/main/resources/. build/classes/
fi
exec java -cp build/classes borrowedenergy.Main "$@"
