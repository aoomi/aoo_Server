#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"; BUILD="$ROOT/work/benchmark/migreal01"; mkdir -p "$BUILD"
JDBC_JAR="${JDBC_JAR:-$(find "$HOME/.m2/repository/com/mysql/mysql-connector-j" -name 'mysql-connector-j-*.jar' -type f | sort -V | tail -1)}"
javac -cp "$JDBC_JAR" -d "$BUILD" "$ROOT/tools/AccountLoginIndexBenchmark.java"
java -cp "$BUILD:$JDBC_JAR" AccountLoginIndexBenchmark "${JDBC_URL:-jdbc:mysql://127.0.0.1:3306/aoo_account_index_verify?useSSL=false&serverTimezone=UTC}" "${JDBC_USER:-root}" "${JDBC_PASSWORD:-Quer_1234}" 2000 "$ROOT/docs/generated/migreal01-account-login-index.json"
