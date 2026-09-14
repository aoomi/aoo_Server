#!/usr/bin/env bash
set -euo pipefail

ROOT="${F03_ROOT:-$(cd "$(dirname "$0")/.." && pwd)}"
OUT="${F03_OUTPUT_DIR:-$ROOT/work/runtime/account-soak-24h-final}"
TARGET_SECONDS="${F03_TARGET_SECONDS:-86400}"
MYSQL_PASSWORD="${AOO_MIGRATION_MYSQL_PASSWORD:?AOO_MIGRATION_MYSQL_PASSWORD is required}"

cd "$ROOT"

while :; do
  elapsed="$(awk -F, 'NR>1 { value=$2 } END { print value+0 }' "$OUT/metrics.csv" 2>/dev/null || printf '0')"
  if (( elapsed >= TARGET_SECONDS )); then
    break
  fi
  sleep 60
done

python3 tools/finalize_f03_soak.py \
  "$OUT/metrics.csv" \
  "$OUT/workload/protocol-smoke.log" \
  --deep-workload "$OUT/workload/two-player-smoke.log" \
  --target-seconds "$TARGET_SECONDS" \
  --output "$OUT/final-report.json"

AOO_MIGRATION_MYSQL_PASSWORD="$MYSQL_PASSWORD" \
  python3 tools/f03_data_evidence.py \
    --compare "$OUT/database-baseline.json" \
    --output "$OUT/database-final.json"

python3 tools/f03_account_id_collision_check.py \
  --output "$OUT/account-id-collision-final.json"

date -u +%FT%TZ > "$OUT/finalized-utc.txt"

shasum -a 256 \
  "$OUT/metrics.csv" \
  "$OUT/workload/protocol-smoke.log" \
  "$OUT/workload/two-player-smoke.log" \
  "$OUT/database-baseline.json" \
  "$OUT/database-final.json" \
  "$OUT/account-id-collision-final.json" \
  "$OUT/final-report.json" \
  "$OUT/evidence/nmt-baseline-summary.txt" \
  "$OUT/finalized-utc.txt" \
  > "$OUT/evidence-sha256.txt"

printf 'F03 local 24-hour evidence finalized: %s\n' "$OUT"
