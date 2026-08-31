#!/bin/zsh
set -eu

ROOT="${0:A:h:h}"
OUT="${SOAK_OUTPUT_DIR:-$ROOT/work/runtime/account-soak-24h}"
DURATION_SECONDS="${SOAK_DURATION_SECONDS:-86400}"
SAMPLE_SECONDS="${SOAK_SAMPLE_SECONDS:-60}"
PORT="${SOAK_PORT:-904}"
PID="${SOAK_PID:-}"
JCMD="${SOAK_JCMD:-$ROOT/../.toolchains/jdk-26.0.2.1.jdk/Contents/Home/bin/jcmd}"
DEPENDENCY_PORTS="${SOAK_DEPENDENCY_PORTS:-3306 16379 19876 10911 27017}"

if [[ -z "$PID" ]]; then
  PID=$(lsof -nP -iTCP:"$PORT" -sTCP:LISTEN -t 2>/dev/null | head -1)
fi
[[ -n "$PID" ]] || { print -u2 "no service is listening on port $PORT"; exit 2; }

mkdir -p "$OUT"
: > "$OUT/metrics.csv"
: > "$OUT/result.md"
print 'timestamp,elapsed_seconds,pid_alive,port_open,dependency_ports_ok,http_status,rss_kib,heap_used_kib,native_committed_kib,threads,open_files,probe_ok' > "$OUT/metrics.csv"
print "$PID" > "$OUT/pid"

START_EPOCH=$(date +%s)
START_TEXT=$(date '+%Y-%m-%d %H:%M:%S %z')
STATUS=RUNNING

while [[ "$STATUS" == RUNNING ]]; do
  NOW=$(date +%s)
  ELAPSED=$((NOW - START_EPOCH))
  ALIVE=0 PORT_OPEN=0 DEPENDENCIES_OK=1 PROBE_OK=0 HTTP_STATUS=000 RSS=0 HEAP_USED=0 NATIVE_COMMITTED=0 THREADS=0 OPEN_FILES=0
  kill -0 "$PID" 2>/dev/null && ALIVE=1
  nc -z 127.0.0.1 "$PORT" 2>/dev/null && PORT_OPEN=1
  for dependency_port in ${(z)DEPENDENCY_PORTS}; do
    nc -z 127.0.0.1 "$dependency_port" 2>/dev/null || DEPENDENCIES_OK=0
  done
  if (( ALIVE == 1 )); then
    RSS=$(ps -o rss= -p "$PID" 2>/dev/null | tr -d ' ' || true)
    THREADS=$(ps -M "$PID" 2>/dev/null | wc -l | tr -d ' ' || true)
    OPEN_FILES=$(lsof -p "$PID" 2>/dev/null | wc -l | tr -d ' ' || true)
    if [[ -x "$JCMD" ]]; then
      HEAP_USED=$("$JCMD" "$PID" GC.heap_info 2>/dev/null | sed -n 's/.* used \([0-9][0-9]*\)K.*/\1/p' | head -1 || true)
      HEAP_USED=${HEAP_USED:-0}
      NATIVE_COMMITTED=$("$JCMD" "$PID" VM.native_memory summary 2>/dev/null | sed -n 's/^Total:.*committed=\([0-9][0-9]*\)KB.*/\1/p' | head -1 || true)
      NATIVE_COMMITTED=${NATIVE_COMMITTED:-0}
    fi
  fi
  if [[ -n "${SOAK_PROBE_COMMAND:-}" ]]; then
    zsh -lc "$SOAK_PROBE_COMMAND" >/dev/null 2>&1 && PROBE_OK=1
  else
    HTTP_STATUS=$(curl -sS --max-time 5 -o /dev/null -w '%{http_code}' "http://127.0.0.1:$PORT/health" 2>/dev/null || print 000)
    [[ "$HTTP_STATUS" != 000 ]] && PROBE_OK=1
  fi
  print "$(date '+%Y-%m-%dT%H:%M:%S%z'),$ELAPSED,$ALIVE,$PORT_OPEN,$DEPENDENCIES_OK,$HTTP_STATUS,${RSS:-0},$HEAP_USED,$NATIVE_COMMITTED,$THREADS,$OPEN_FILES,$PROBE_OK" >> "$OUT/metrics.csv"
  if (( ALIVE == 0 || PORT_OPEN == 0 || DEPENDENCIES_OK == 0 || PROBE_OK == 0 )); then
    STATUS=FAILED_RUNTIME
    break
  fi
  if (( ELAPSED >= DURATION_SECONDS )); then
    STATUS=PASSED
    break
  fi
  sleep "$SAMPLE_SECONDS"
done

END_TEXT=$(date '+%Y-%m-%d %H:%M:%S %z')
LAST_METRIC=$(tail -1 "$OUT/metrics.csv")
cat > "$OUT/result.md" <<EOF
# Account Server Soak Result

- Status: $STATUS
- Started: $START_TEXT
- Finished: $END_TEXT
- Target seconds: $DURATION_SECONDS
- PID: $PID
- Port: $PORT
- Last metric: \`$LAST_METRIC\`
- Metrics: \`$OUT/metrics.csv\`
EOF

[[ "$STATUS" == PASSED ]]
