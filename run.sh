#!/usr/bin/env bash
# Bash launcher: compiles with javac then runs.
# Requires JDK 21+ in PATH.

set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"
mkdir -p "$OUT"

find "$ROOT/src/main/java" -name "*.java" > "$ROOT/.sources.txt"
echo "Compiling $(wc -l < "$ROOT/.sources.txt") source files..."
javac -d "$OUT" --release 21 @"$ROOT/.sources.txt"

echo "Launching..."
java -cp "$OUT" com.pathfinding.Main
