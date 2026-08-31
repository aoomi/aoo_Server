#!/usr/bin/env bash
set -uo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
LOG_DIR="$ROOT/work/v01"
mkdir -p "$LOG_DIR"

cd "$ROOT"
gate_status=0
./mvnw clean verify 2>&1 | tee "$LOG_DIR/server-gated-reactor.log" || gate_status=$?

# A failed root validation gate stops Maven before any child is built. Run the
# reactor once more with only exec-based repository audits disabled so compile,
# Surefire tests and packaging for every declared module are still exercised.
reactor_status=0
./mvnw -Dexec.skip=true verify 2>&1 | tee "$LOG_DIR/server-full-reactor.log" || reactor_status=$?

printf 'root-gates=%s full-reactor=%s\n' "$gate_status" "$reactor_status" | tee "$LOG_DIR/status.txt"
if (( gate_status != 0 || reactor_status != 0 )); then
  exit 1
fi
