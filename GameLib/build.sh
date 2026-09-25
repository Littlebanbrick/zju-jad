#!/usr/bin/env bash
# 编译 + 打包。用法：./build.sh
set -euo pipefail
cd "$(dirname "$0")"

OUT=out
JAR=game-lib.jar
MAIN=gamelib.Main

rm -rf "$OUT"
mkdir -p "$OUT"

mapfile -t SOURCES < <(find src -name '*.java')
echo "编译 ${#SOURCES[@]} 个源文件 ..."

# --release 17 : 产出 Java 17 字节码，老 JDK 也能跑（你本机是 JDK 25，默认会产出 69 版）
# -encoding    : 不加就按平台默认编码读源码，中文注释和字符串会烂掉
# -Xlint:all   : 打开全部警告
javac --release 17 -encoding UTF-8 -Xlint:all -d "$OUT" "${SOURCES[@]}"

echo "打包 -> $JAR ..."
# -C "$OUT" .  : 先切到 out/，再把里面的内容加进去。
#                这样 jar 根目录下是 gamelib/...，与 package 结构对齐。
#                写成 `jar cf $JAR out/` 会把 out/ 这个名字本身也打进去，运行找不到主类。
jar --create --file "$JAR" --main-class "$MAIN" -C "$OUT" .

echo "完成。运行： java -jar $JAR"
