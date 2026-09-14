#!/usr/bin/env bash
set -euo pipefail
R="$(cd "$(dirname "$0")/.."&&pwd)";B="$R/work/benchmark/migreal09";mkdir -p "$B";J="${JDBC_JAR:-$(find "$HOME/.m2/repository/com/mysql/mysql-connector-j" -name 'mysql-connector-j-*.jar' -type f|sort -V|tail -1)}";javac -cp "$J" -d "$B" "$R/tools/LedgerReconciliationBenchmark.java";java -cp "$B:$J" LedgerReconciliationBenchmark "${JDBC_URL:-jdbc:mysql://127.0.0.1:3306/aoo_migreal09?useSSL=false&serverTimezone=UTC}" "${JDBC_USER:-root}" "${JDBC_PASSWORD:-Quer_1234}" 5000 "$R/docs/generated/migreal09-ledger-reconciliation.json"
