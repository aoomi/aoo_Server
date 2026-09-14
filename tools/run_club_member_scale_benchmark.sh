#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; BUILD="$ROOT/work/benchmark/migreal02"; mkdir -p "$BUILD"
JAR="${JDBC_JAR:-$(find "$HOME/.m2/repository/com/mysql/mysql-connector-j" -name 'mysql-connector-j-*.jar' -type f|sort -V|tail -1)}"
javac -cp "$JAR" -d "$BUILD" "$ROOT/tools/ClubMemberScaleBenchmark.java"
java -cp "$BUILD:$JAR" ClubMemberScaleBenchmark "${JDBC_URL:-jdbc:mysql://127.0.0.1:3306/aoo_migreal02?useSSL=false&serverTimezone=UTC}" "${JDBC_USER:-root}" "${JDBC_PASSWORD:-Quer_1234}" 5000 "$ROOT/docs/generated/migreal02-club-member-scale.json"
