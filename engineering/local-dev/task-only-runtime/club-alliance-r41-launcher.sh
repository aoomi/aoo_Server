#!/bin/bash
# Task-owned process control. This launcher never prepares or migrates a database.
set -euo pipefail
action=${1:-status}
config=${2:-}
fail() { printf '%s\n' "ClubAlliance R41: $*" >&2; exit 1; }
[[ -n "$config" && -f "$config" ]] || fail 'provide an isolated configuration file as argument 2'
while IFS= read -r line || [[ -n "$line" ]]; do
  [[ -z "$line" || "$line" == \#* ]] && continue
  [[ "$line" == *=* ]] || fail 'invalid configuration line'
  key=${line%%=*}; value=${line#*=}
  case "$key" in
    R41_STATE_DIR|R41_RUNNER|R41_RUNNER_SHA256|R41_DB_RECEIPT|R41_DB_RECEIPT_SHA256|R41_DB_HOST|R41_DB_PORT|R41_DB_SCHEMA|R41_DB_PASSWORD_FILE) export "$key=$value" ;;
    *) fail "unsupported configuration key: $key" ;;
  esac
done < "$config"
[[ "${R41_STATE_DIR:-}" == /*/club-alliance-r41 ]] || fail 'state directory must be absolute and end in /club-alliance-r41'
pidfile="$R41_STATE_DIR/runtime.pid"
case "$action" in
  status)
    if [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then printf 'RUNNING\n'; else printf 'STOPPED\n'; fi ;;
  start)
    [[ "${R41_DB_HOST:-}" == 127.0.0.1 ]] || fail 'isolated database must use loopback'
    [[ "${R41_DB_PORT:-}" =~ ^[0-9]+$ && "$R41_DB_PORT" != 3306 ]] || fail 'provide a dedicated database port'
    if [[ "${R41_DB_SCHEMA:-}" == aoo_root_lease_r16 ]]; then
      [[ "$R41_DB_PORT" == 13369 ]] || fail 'original schema is permitted only on the dedicated clone port'
      clone_mount=$(docker inspect --format '{{range .Mounts}}{{if eq .Destination "/var/lib/mysql"}}{{.Name}}{{end}}{{end}}' aoo-club-alliance-r41-mysql)
      [[ "$clone_mount" == aoo-club-alliance-r41-db ]] || fail 'dedicated clone volume ownership mismatch'
      clone_port=$(docker port aoo-club-alliance-r41-mysql 3306/tcp)
      [[ "$clone_port" == 127.0.0.1:13369 ]] || fail 'dedicated clone port ownership mismatch'
    else
      [[ "${R41_DB_SCHEMA:-}" == aoo_club_alliance_r41* ]] || fail 'provide a task-owned database schema'
    fi
    [[ -f "${R41_DB_PASSWORD_FILE:-}" ]] || fail 'database password file is missing'
    [[ -f "${R41_DB_RECEIPT:-}" && "${R41_DB_RECEIPT_SHA256:-}" =~ ^[a-f0-9]{64}$ ]] || fail 'authoritative database provenance receipt and SHA256 are required'
    [[ "$(shasum -a 256 "$R41_DB_RECEIPT" | cut -d ' ' -f 1)" == "$R41_DB_RECEIPT_SHA256" ]] || fail 'database receipt hash mismatch'
    [[ "${R41_RUNNER:-}" == /* && -x "$R41_RUNNER" && "${R41_RUNNER_SHA256:-}" =~ ^[a-f0-9]{64}$ ]] || fail 'approved executable isolated runner and SHA256 are required'
    case "$R41_RUNNER" in *local-dev-services*|*local_service_control*) fail 'shared startup scripts are forbidden' ;; esac
    [[ "$(shasum -a 256 "$R41_RUNNER" | cut -d ' ' -f 1)" == "$R41_RUNNER_SHA256" ]] || fail 'runner hash mismatch'
    if [[ -f "$pidfile" ]] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then fail 'owned runtime is already running'; fi
    umask 077
    mkdir -p "$R41_STATE_DIR"
    nohup "$R41_RUNNER" > "$R41_STATE_DIR/runtime.log" 2>&1 &
    printf '%s\n' "$!" > "$pidfile"
    printf 'START_REQUESTED; readiness must be verified separately\n' ;;
  stop)
    [[ -f "$pidfile" ]] || fail 'no owned PID receipt'
    pid=$(cat "$pidfile")
    [[ "$pid" =~ ^[0-9]+$ ]] || fail 'invalid PID receipt'
    command_line=$(ps -p "$pid" -o command= || true)
    [[ -n "${R41_RUNNER:-}" && "$command_line" == *"$R41_RUNNER"* ]] || fail 'PID ownership cannot be confirmed; no process stopped'
    kill "$pid"
    rm "$pidfile" ;;
  *) fail 'action must be start, status or stop' ;;
esac
