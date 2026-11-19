#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=runner-r42.sh
source "$script_dir/runner-r42.sh"

tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/aoo-r42-readiness-test.XXXXXX")"
port_file="$tmp_dir/port"
trap 'if [[ -n "${server_pid:-}" ]]; then kill "$server_pid" 2>/dev/null || true; wait "$server_pid" 2>/dev/null || true; fi; rm -rf "$tmp_dir"' EXIT

python3 -u - "$port_file" <<'PY' &
import http.server
import pathlib
import sys
import time

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/slow":
            time.sleep(2)
        if self.path == "/health/gateway":
            status = 200
            body = b'{"status":"ready","code":"GATEWAY_READY"}'
        elif self.path == "/internal/v1/health/room-authority":
            status = 200 if self.headers.get("X-Aoo-Internal-Token") == "test-token" else 403
            body = b'{"status":"ready","code":"ROOM_AUTHORITY_READY"}' if status == 200 else b'{"status":"not_ready","code":"INTERNAL_AUTHORITY_FORBIDDEN"}'
        elif self.path == "/not-ready":
            status = 503
            body = b'{"status":"not_ready","code":"ROOM_AUTHORITY_NOT_READY"}'
        elif self.path == "/wrong-body":
            status = 200
            body = b'{"status":"not_ready","code":"ROOM_AUTHORITY_NOT_READY"}'
        else:
            status = 404
            body = b'{"status":"not_ready","code":"NOT_FOUND"}'
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)
    def log_message(self, *_):
        pass

server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
pathlib.Path(sys.argv[1]).write_text(str(server.server_port), encoding="ascii")
server.serve_forever()
PY
server_pid=$!
for _ in {1..50}; do [[ -s "$port_file" ]] && break; kill -0 "$server_pid" 2>/dev/null || exit 1; sleep 0.02; done
[[ -s "$port_file" ]] || exit 1
port="$(<"$port_file")"
R42_CURL_BIN="$(command -v curl)"
R42_JSON_BIN="$(command -v python3)"
R42_READY_TIMEOUT_SECONDS=1
R42_ATTEMPT_ROOT="$tmp_dir"
base="http://127.0.0.1:$port"

[[ "$(readiness_http_status "$base/health/gateway" "")" == "200" ]]
[[ "$(readiness_http_status "$base/internal/v1/health/room-authority" "$tmp_dir/token" 2>/dev/null || true)" != "200" ]]
printf '%s' 'test-token' > "$tmp_dir/token"
[[ "$(readiness_http_status "$base/internal/v1/health/room-authority" "$tmp_dir/token")" == "200" ]]
[[ "$(readiness_http_status "$base/not-ready" "")" == "503" ]]
if (require_readiness_200 "$base/wrong-body" "" GATEWAY_READINESS) >/dev/null 2>&1; then
  echo "expected HTTP 200 wrong-body rejection" >&2
  exit 1
fi
if readiness_http_status "$base/slow" "" >/dev/null 2>&1; then
  echo "expected curl timeout" >&2
  exit 1
fi

# Status must report the persisted state, never infer success from the command
# name or manufacture a READY record. These checks use only task-local files.
status_root="$tmp_dir/status-attempt"
mkdir -p "$status_root/state" "$status_root/bundle" "$status_root/runners" "$status_root/logs" "$status_root/pids"
status_config="$status_root/config.env"
cat > "$status_config" <<EOF
R42_CONFIG_PATH=$status_config
R42_ATTEMPT_ROOT=$status_root
R42_BUNDLE_ROOT=$status_root/bundle
R42_RUNNER_ROOT=$status_root/runners
R42_LOG_ROOT=$status_root/logs
R42_PID_ROOT=$status_root/pids
R42_STATE_ROOT=$status_root/state
R42_JSON_BIN=$(command -v python3)
EOF
set +e
missing_status_output="$(bash "$script_dir/runner-r42.sh" status "$status_config" 2>&1)"
missing_status_rc=$?
set -e
[[ "$missing_status_rc" == 78 ]]
[[ "$missing_status_output" == *"code=STATUS_NOT_STARTED"* ]]
printf '%s\n' '{"status":"PREFLIGHT","code":"PREFLIGHT_OK","stage":"PREFLIGHT","timestamp":"2026-10-11T00:00:00Z","message":"task-local"}' > "$status_root/state/runner-status.json"
status_preimage="$(shasum -a 256 "$status_root/state/runner-status.json" | awk '{print $1}')"
set +e
preflight_status_output="$(bash "$script_dir/runner-r42.sh" status "$status_config" 2>&1)"
preflight_status_rc=$?
set -e
[[ "$preflight_status_rc" == 78 ]]
[[ "$preflight_status_output" == *"status=PREFLIGHT code=PREFLIGHT_OK"* ]]
[[ "$(shasum -a 256 "$status_root/state/runner-status.json" | awk '{print $1}')" == "$status_preimage" ]]
echo "runner-r42 readiness contract: PASS"
