#!/usr/bin/env bash
set -euo pipefail
R="$(cd "$(dirname "$0")/.."&&pwd)";B="$R/work/benchmark/migreal07";mkdir -p "$B";J="${JDBC_JAR:-$(find "$HOME/.m2/repository/com/mysql/mysql-connector-j" -name 'mysql-connector-j-*.jar' -type f|sort -V|tail -1)}";javac -cp "$J" -d "$B" "$R/tools/PlayerScaleBenchmark.java";java -cp "$B:$J" PlayerScaleBenchmark "${JDBC_URL:-jdbc:mysql://127.0.0.1:3306/aoo_account_index_verify?useSSL=false&serverTimezone=UTC}" "${JDBC_USER:-root}" "${JDBC_PASSWORD:-Quer_1234}" 5000 "$R/docs/generated/migreal07-player-scale.json"
