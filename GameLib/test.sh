#!/usr/bin/env bash
# 编译 + 跑测试。用法：./test.sh
set -euo pipefail
cd "$(dirname "$0")"

OUT=out
ENTRY=gamelib.test.SelfTest

mkdir -p "$OUT"

mapfile -t SOURCES < <(find src -name '*.java')
javac --release 17 -encoding UTF-8 -Xlint:all -d "$OUT" "${SOURCES[@]}"

# 只编到 out/，不打包 —— 测试用 classpath 直接跑，不需要 jar
java -cp "$OUT" "$ENTRY"
