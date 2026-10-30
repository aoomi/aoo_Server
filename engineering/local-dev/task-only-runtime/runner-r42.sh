#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'
umask 077

# R42 is a new task-only contract. It does not modify or call launcher-r20.sh.
# Every input is explicit; absence, a placeholder, a source/bundle admission
# blocker, or an unproven readiness route stops the graph before any start.

readonly SCRIPT_NAME="runner-r42.sh"
readonly REQUIRED_PREFIX="/tmp/aoo-c3-runtime-matrix-r42-"
CONFIG_PATH=""
CURRENT_STAGE="NOT_STARTED"
STATUS_FILE=""

emit_status() {
  local status="${1:-BLOCKED}" code="${2:-UNKNOWN}" message="${3:-}" now
  now="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  if [[ -n "$STATUS_FILE" ]]; then
    mkdir -p "$(dirname "$STATUS_FILE")" 2>/dev/null || true
    printf '{"status":"%s","code":"%s","stage":"%s","timestamp":"%s","message":"%s"}\n' \
      "$status" "$code" "$CURRENT_STAGE" "$now" "${message//\"/\\\"}" > "$STATUS_FILE" 2>/dev/null || true
  fi
  printf 'R42_GRAPH status=%s code=%s stage=%s message=%s\n' "$status" "$code" "$CURRENT_STAGE" "$message" >&2
}

die() {
  local code="$1" message="$2"
  CURRENT_STAGE="BLOCKED"
  emit_status BLOCKED "$code" "$message"
  exit 78
}

usage() { echo "usage: $SCRIPT_NAME {preflight|start|status|cleanup} /tmp/aoo-c3-runtime-matrix-r42-<attempt>/config.env" >&2; }
require_var() { [[ -n "${!1:-}" ]] || die "MISSING_INPUT_$1" "required input is missing: $1"; }
is_placeholder() { case "$1" in REQUIRED__*|*'<'*|*'>'*|/absolute/path/*|*'<attempt>'*) return 0;; *) return 1;; esac; }
canonical() { "$R42_JSON_BIN" -c 'import os,sys; print(os.path.realpath(sys.argv[1]),end="")' "$1" 2>/dev/null; }
under() {
  local p r rel
  p="$(canonical "$1")"; r="$(canonical "$2")"
  [[ -n "$p" && -n "$r" ]] || return 1
  rel="$("$R42_JSON_BIN" -c 'import os,sys; print(os.path.relpath(sys.argv[1],sys.argv[2]),end="")' "$p" "$r")"
  [[ "$rel" != ".." && "$rel" != ../* ]]
}

load_config() {
  local line key value
  [[ -f "$CONFIG_PATH" && ! -L "$CONFIG_PATH" ]] || die CONFIG_NOT_REGULAR "config is not a regular non-symlink file"
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" || "$line" == \#* ]] && continue
    [[ "$line" == *=* ]] || die CONFIG_LINE_INVALID "config lines must be literal KEY=VALUE assignments"
    key="${line%%=*}"; value="${line#*=}"
    [[ "$key" =~ ^[A-Z][A-Z0-9_]+$ ]] || die CONFIG_KEY_INVALID "invalid key: $key"
    [[ -z "${!key+x}" ]] || die CONFIG_KEY_DUPLICATE "duplicate key: $key"
    case "$value" in *'$('|*'`'*|*';'*|*'&&'*|*'||'*|*'>'*|*'<'*) die CONFIG_VALUE_NOT_LITERAL "shell expression in $key";; esac
    is_placeholder "$value" && die PLACEHOLDER_INPUT "$key still contains a placeholder"
    export "$key=$value"
  done < "$CONFIG_PATH"
}

validate_attempt() {
  require_var R42_ATTEMPT_ROOT; require_var R42_BUNDLE_ROOT; require_var R42_RUNNER_ROOT
  require_var R42_LOG_ROOT; require_var R42_PID_ROOT; require_var R42_STATE_ROOT
  case "$R42_ATTEMPT_ROOT" in "$REQUIRED_PREFIX"*) ;; *) die ATTEMPT_ROOT_NOT_ISOLATED "attempt root must use the R42 /tmp prefix";; esac
  [[ -d "$R42_ATTEMPT_ROOT" && ! -L "$R42_ATTEMPT_ROOT" ]] || die ATTEMPT_ROOT_INVALID "attempt root must already be a real directory"
  for p in "$R42_BUNDLE_ROOT" "$R42_RUNNER_ROOT" "$R42_LOG_ROOT" "$R42_PID_ROOT" "$R42_STATE_ROOT"; do
    under "$p" "$R42_ATTEMPT_ROOT" || die ATTEMPT_CHILD_OUTSIDE "attempt child is outside attempt root"
  done
  [[ "$CONFIG_PATH" == "$R42_CONFIG_PATH" ]] || die CONFIG_PATH_MISMATCH "config argument differs from R42_CONFIG_PATH"
  under "$CONFIG_PATH" "$R42_ATTEMPT_ROOT" || die CONFIG_OUTSIDE_ATTEMPT "config must be under attempt root"
  STATUS_FILE="$R42_STATE_ROOT/runner-status.json"
}

validate_tools() {
  local n
  for n in R42_JAVA_BIN R42_SHA256_BIN R42_JSON_BIN R42_CURL_BIN R42_DOCKER_BIN R42_PS_BIN R42_PROCESS_CONTROL_BIN; do
    require_var "$n"; [[ -x "${!n}" && "${!n}" == /* ]] || die HOST_TOOL_INVALID "host tool is not an explicit executable: $n"
  done
  require_var R42_JAVA_EXPECTED_MAJOR; [[ "$R42_JAVA_EXPECTED_MAJOR" == 25 ]] || die JAVA_MAJOR_CONTRACT "Java 25 is required"
  require_var R42_TIMEZONE; [[ "$R42_TIMEZONE" == UTC ]] || die TIMEZONE_CONTRACT "UTC is required"
  "$R42_JAVA_BIN" -version 2>&1 | rg -q 'version "25([.]|"|$)' || die JAVA_MAJOR_MISMATCH "configured Java is not Java 25"
  case "$(basename "$R42_SHA256_BIN")" in sha256sum|shasum) ;; *) die HASH_TOOL_INVALID "SHA tool must be sha256sum or shasum";; esac
}

sha256_file() {
  case "$(basename "$R42_SHA256_BIN")" in
    sha256sum) "$R42_SHA256_BIN" "$1" | awk '{print $1}';;
    shasum) "$R42_SHA256_BIN" -a 256 "$1" | awk '{print $1}';;
  esac
}

validate_bundle() {
  require_var R42_SOURCE_BUNDLE_ROOT; require_var R42_BUNDLE_MANIFEST_SHA256; require_var R42_JAR_MANIFEST_SHA256
  require_var R42_RUNTIME_MANIFEST_SHA256; require_var R42_EXPECTED_PAYLOAD_COUNT
  [[ -d "$R42_SOURCE_BUNDLE_ROOT" && ! -L "$R42_SOURCE_BUNDLE_ROOT" ]] || die SOURCE_BUNDLE_MISSING "R40 source bundle is not present"
  local manifest="$R42_SOURCE_BUNDLE_ROOT/bundle-manifest-r38.json" jars="$R42_SOURCE_BUNDLE_ROOT/jars.sha256"
  [[ -f "$manifest" && -f "$jars" ]] || die BUNDLE_CONTRACT_FILES_MISSING "R40 bundle manifest or hash list is missing"
  [[ "$(sha256_file "$manifest")" == "$R42_BUNDLE_MANIFEST_SHA256" ]] || die BUNDLE_MANIFEST_SHA_MISMATCH "R40 bundle manifest bytes differ"
  [[ "$(sha256_file "$jars")" == "$R42_JAR_MANIFEST_SHA256" ]] || die BUNDLE_HASH_MANIFEST_SHA_MISMATCH "R40 jars.sha256 bytes differ"
  [[ "$(wc -l < "$jars" | tr -d ' ')" == "$R42_EXPECTED_PAYLOAD_COUNT" ]] || die BUNDLE_PAYLOAD_COUNT_MISMATCH "sealed payload count differs"
  [[ "$(sha256_file "$R42_SOURCE_BUNDLE_ROOT/runtime-metadata-canonical-manifest.json")" == "$R42_RUNTIME_MANIFEST_SHA256" ]] || die RUNTIME_MANIFEST_SHA_MISMATCH "embedded runtime manifest differs"
  (cd "$R42_SOURCE_BUNDLE_ROOT" && "$R42_SHA256_BIN" --strict -c jars.sha256 >/dev/null 2>&1) || die BUNDLE_PAYLOAD_HASH_FAILED "R40 payload hash check failed"
  if ! "$R42_JSON_BIN" - "$manifest" "$R42_SOURCE_BUNDLE_ROOT/game-bootstrap.jar" "$R42_SOURCE_BUNDLE_ROOT/game-gateway.jar" <<'PY'
import json,sys,zipfile
manifest,bootstrap,gateway=sys.argv[1:]
doc=json.load(open(manifest,encoding="utf-8"))
if doc.get("status") != "CANDIDATE_FAIL_CLOSED": raise SystemExit("bundle status changed unexpectedly")
if "WORDCARD_ARTIFACT_PRESENT" in doc.get("admissionBlockers",[]): raise SystemExit("WORDCARD_ARTIFACT_PRESENT")
if any(x in doc.get("admissionBlockers",[]) for x in ("BLOCKED_RUNNER_SERVICE_GRAPH_CONTRACT","BLOCKED_MISSING_TASK_ISOLATED_GAME_WS_AUTHORITY")): raise SystemExit("runtime admission blocker")
for path,main in ((bootstrap,"com.aoo.bcg.bootstrap.BootstrapAPP"),(gateway,"com.aoo.bcg.gateway.GatewayApplication")):
    with zipfile.ZipFile(path) as z:
        text=z.read("META-INF/MANIFEST.MF").decode("utf-8","replace")
        if "Main-Class: "+main not in text: raise SystemExit("missing main class")
        if path==gateway:
            classes=b"".join(z.read(n) for n in z.namelist() if n.endswith(".class"))
            if b"/health/gateway" not in classes: raise SystemExit("GATEWAY_HEALTH_ROUTE_UNPROVEN")
            if b"/internal/v1/health/room-authority" not in classes: raise SystemExit("AUTHORITY_HEALTH_ROUTE_UNPROVEN")
PY
  then
    die BUNDLE_ADMISSION_BLOCKED "R40 bundle remains candidate/fail-closed or lacks a proven entrypoint/health contract"
  fi
}

validate_graph_manifest() {
  require_var R42_GRAPH_MANIFEST; require_var R42_GRAPH_MANIFEST_SHA256
  [[ -f "$R42_GRAPH_MANIFEST" && ! -L "$R42_GRAPH_MANIFEST" ]] || die GRAPH_MANIFEST_MISSING "R42 graph manifest is not a regular file"
  [[ "$(sha256_file "$R42_GRAPH_MANIFEST")" == "$R42_GRAPH_MANIFEST_SHA256" ]] || die GRAPH_MANIFEST_SHA_MISMATCH "graph manifest bytes differ"
  if ! "$R42_JSON_BIN" - "$R42_GRAPH_MANIFEST" <<'PY'
import json,sys
doc=json.load(open(sys.argv[1],encoding="utf-8"))
if doc.get("graphKind")!="task-only-service-graph" or doc.get("graphVersion")!="r42-v1": raise SystemExit("graph identity invalid")
nodes={n["id"] for n in doc.get("nodes",[])}
required={"db","s3","version","account","billing","inventory","referral","social","gifting","media","gateway","hall"}
if nodes != required: raise SystemExit("graph nodes incomplete")
if doc.get("startOrder") != ["db","s3","billing","inventory","referral","version","account","social","gifting","media","gateway","hall"]: raise SystemExit("start order invalid")
if doc.get("cleanupOrder") != ["hall","gateway","media","gifting","social","account","version","referral","inventory","billing","s3","db"]: raise SystemExit("cleanup order invalid")
PY
  then
    die GRAPH_MANIFEST_INVALID "R42 graph manifest failed strict structural validation"
  fi
}

validate_ports() {
  local n p seen=""; local -a names=(R42_CLIENT_PORT R42_DB_PORT R42_S3_PORT R42_VERSION_PORT R42_ACCOUNT_PORT R42_BILLING_PORT R42_INVENTORY_PORT R42_REFERRAL_PORT R42_HALL_PORT R42_SOCIAL_PORT R42_GIFTING_PORT R42_MEDIA_PORT R42_GATEWAY_PORT R42_AUTHORITY_PORT)
  for n in "${names[@]}"; do require_var "$n"; p="${!n}"; [[ "$p" =~ ^[0-9]+$ && "$p" -ge 1024 && "$p" -le 65535 ]] || die PORT_INVALID "invalid task port $n"; case " $seen " in *" $p "*) die PORT_DUPLICATE "duplicate task port $p";; esac; seen="$seen $p"; done
  local reserved; for reserved in 3306 8080 8093 8095 8096 8097 8101 8102 18080; do [[ " $seen " != *" $reserved "* ]] || die SHARED_PORT_FORBIDDEN "reserved shared port is assigned: $reserved"; done
}

validate_urls() {
  local pair service port url; local -a pairs=("VERSION:R42_VERSION_PORT:R42_VERSION_READY_URL" "ACCOUNT:R42_ACCOUNT_PORT:R42_ACCOUNT_READY_URL" "BILLING:R42_BILLING_PORT:R42_BILLING_READY_URL" "INVENTORY:R42_INVENTORY_PORT:R42_INVENTORY_READY_URL" "REFERRAL:R42_REFERRAL_PORT:R42_REFERRAL_READY_URL" "HALL:R42_HALL_PORT:R42_HALL_READY_URL" "SOCIAL:R42_SOCIAL_PORT:R42_SOCIAL_READY_URL" "GIFTING:R42_GIFTING_PORT:R42_GIFTING_READY_URL" "MEDIA:R42_MEDIA_PORT:R42_MEDIA_READY_URL" "GATEWAY:R42_GATEWAY_PORT:R42_GATEWAY_READY_URL")
  for pair in "${pairs[@]}"; do IFS=: read -r service port url <<< "$pair"; require_var "$port"; require_var "$url"; [[ "${!url}" == "http://127.0.0.1:${!port}"/* || "${!url}" == "http://127.0.0.1:${!port}" ]] || die READINESS_URL_MISMATCH "$url is not task-local to its port"; done
  require_var R42_AUTHORITY_READY_URL; [[ "$R42_AUTHORITY_READY_URL" == "http://127.0.0.1:${R42_AUTHORITY_PORT}"/* || "$R42_AUTHORITY_READY_URL" == "http://127.0.0.1:${R42_AUTHORITY_PORT}" ]] || die AUTHORITY_URL_MISMATCH "authority readiness is not task-local"
  require_var R42_OFFICIAL_ORIGINS; [[ "$R42_OFFICIAL_ORIGINS" == "http://127.0.0.1:${R42_CLIENT_PORT}" ]] || die ORIGIN_CONTRACT_INVALID "official origin is not the exact task origin"
  require_var R42_READY_TIMEOUT_SECONDS
  [[ "$R42_READY_TIMEOUT_SECONDS" =~ ^[0-9]+$ && "$R42_READY_TIMEOUT_SECONDS" -ge 1 && "$R42_READY_TIMEOUT_SECONDS" -le 30 ]] || die READINESS_TIMEOUT_INVALID "readiness timeout must be 1..30 seconds"
  require_var GATEWAY_INTERNAL_TOKEN_FILE
  [[ -f "$GATEWAY_INTERNAL_TOKEN_FILE" && ! -L "$GATEWAY_INTERNAL_TOKEN_FILE" && -s "$GATEWAY_INTERNAL_TOKEN_FILE" ]] || die GATEWAY_READINESS_TOKEN_FILE_INVALID "gateway readiness token file is not a non-empty regular file"
  require_readiness_200 "$R42_GATEWAY_READY_URL" "" GATEWAY_READINESS
  require_readiness_200 "$R42_AUTHORITY_READY_URL" "$GATEWAY_INTERNAL_TOKEN_FILE" AUTHORITY_READINESS
}

readiness_http_status() {
  local url="$1" token_file="${2:-}" token="";
  if [[ -n "$token_file" ]]; then
    token="$(<"$token_file")"
    "$R42_CURL_BIN" --silent --show-error --output /dev/null --write-out '%{http_code}' --max-time "$R42_READY_TIMEOUT_SECONDS" --request GET --header "X-Aoo-Internal-Token: $token" "$url"
  else
    "$R42_CURL_BIN" --silent --show-error --output /dev/null --write-out '%{http_code}' --max-time "$R42_READY_TIMEOUT_SECONDS" --request GET "$url"
  fi
}

require_readiness_200() {
  local url="$1" token_file="$2" label="$3" status="" response="" body="" expected=""
  case "$label" in
    GATEWAY_READINESS) expected="GATEWAY_READY";;
    AUTHORITY_READINESS) expected="ROOM_AUTHORITY_READY";;
    *) die "${label}_CONTRACT_INVALID" "$label is not an admitted readiness contract";;
  esac
  # Capture the body as well as the HTTP status.  A task-only endpoint that
  # returns HTTP 200 with a non-ready or malformed body is not readiness.
  if [[ -n "$token_file" ]]; then
    local token=""
    token="$(<"$token_file")"
    if ! response="$("$R42_CURL_BIN" --silent --show-error --output - --write-out $'\n%{http_code}' --max-time "$R42_READY_TIMEOUT_SECONDS" --request GET --header "X-Aoo-Internal-Token: $token" "$url")"; then
      die "${label}_TIMEOUT_OR_UNREACHABLE" "$label GET did not complete within the configured timeout"
    fi
  else
    if ! response="$("$R42_CURL_BIN" --silent --show-error --output - --write-out $'\n%{http_code}' --max-time "$R42_READY_TIMEOUT_SECONDS" --request GET "$url")"; then
      die "${label}_TIMEOUT_OR_UNREACHABLE" "$label GET did not complete within the configured timeout"
    fi
  fi
  status="${response##*$'\n'}"
  body="${response%$'\n'$status}"
  [[ "$status" =~ ^[0-9]{3}$ ]] || die "${label}_HTTP_INVALID" "$label GET did not return a valid HTTP status"
  [[ "$status" == "200" ]] || die "${label}_HTTP_FAILED" "$label GET returned HTTP $status"
  if ! printf '%s' "$body" | "$R42_JSON_BIN" -c 'import json,sys; value=json.load(sys.stdin); expected=sys.argv[1]; assert isinstance(value,dict) and value.get("status")=="ready" and value.get("code")==expected' "$expected" >/dev/null 2>&1; then
    die "${label}_BODY_INVALID" "$label GET returned HTTP 200 without the expected ready JSON contract"
  fi
}

validate_files_and_relations() {
  local n p; local -a files=(R42_DB_PASSWORD_FILE R42_DB_PREIMAGE_FILE R42_S3_ACCESS_KEY_FILE R42_S3_SECRET_KEY_FILE VERSION_INTERNAL_TOKEN_FILE VERSION_CLIENT_TOKEN_FILE ACCOUNT_OPERATOR_TOKEN_FILE ACCOUNT_IDENTITY_SECOND_FACTOR_TOKEN_FILE ACCOUNT_GATEWAY_INTERNAL_TOKEN_FILE GATEWAY_INTERNAL_TOKEN_FILE BILLING_INTERNAL_TOKEN_FILE BILLING_CALLBACK_SECRET_FILE BILLING_ACCOUNT_AUTH_SECRET_FILE BILLING_PROVIDER_MERCHANT_ID_FILE BILLING_PROVIDER_SIGNING_SECRET_FILE INVENTORY_AUTH_TOKEN_FILE REFERRAL_INTERNAL_TOKEN_FILE REFERRAL_BILLING_TOKEN_FILE HALL_AUTH_SECRET_FILE HALL_INTERNAL_TOKEN_FILE HALL_ROOM_AUTHORITY_TOKEN_FILE INVITE_SIGNING_KEY_FILE INVITE_REFERRAL_TOKEN_FILE MEDIA_CLEANUP_TOKEN_FILE MEDIA_ROOM_TOKEN_FILE MEDIA_S3_ACCESS_KEY_FILE MEDIA_S3_SECRET_KEY_FILE)
  for n in "${files[@]}"; do require_var "$n"; p="${!n}"; under "$p" "$R42_ATTEMPT_ROOT" || die SECRET_PATH_OUTSIDE_ATTEMPT "$n is outside attempt"; [[ -f "$p" && ! -L "$p" && -s "$p" ]] || die SECRET_FILE_MISSING "$n is not a non-empty attempt file"; done
  cmp -s "$ACCOUNT_GATEWAY_INTERNAL_TOKEN_FILE" "$GATEWAY_INTERNAL_TOKEN_FILE" || die TOKEN_RELATION_MISMATCH "Account/Gateway internal token relation failed"
  cmp -s "$HALL_ROOM_AUTHORITY_TOKEN_FILE" "$GATEWAY_INTERNAL_TOKEN_FILE" || die TOKEN_RELATION_MISMATCH "Hall/Gateway authority token relation failed"
  cmp -s "$INVITE_REFERRAL_TOKEN_FILE" "$REFERRAL_INTERNAL_TOKEN_FILE" || die TOKEN_RELATION_MISMATCH "Hall/Referral token relation failed"
  cmp -s "$REFERRAL_BILLING_TOKEN_FILE" "$BILLING_INTERNAL_TOKEN_FILE" || die TOKEN_RELATION_MISMATCH "Referral/Billing token relation failed"
  cmp -s "$INVENTORY_BILLING_TOKEN_FILE" "$BILLING_INTERNAL_TOKEN_FILE" || die TOKEN_RELATION_MISMATCH "Inventory/Billing token relation failed"
  require_var R42_DB_IMAGE; require_var R42_DB_IMAGE_DIGEST; require_var R42_S3_IMAGE; require_var R42_S3_IMAGE_DIGEST
  "$R42_DOCKER_BIN" image inspect "$R42_DB_IMAGE@$R42_DB_IMAGE_DIGEST" >/dev/null 2>&1 || die DB_IMAGE_UNAVAILABLE "explicit MySQL image digest is unavailable locally"
  "$R42_DOCKER_BIN" image inspect "$R42_S3_IMAGE@$R42_S3_IMAGE_DIGEST" >/dev/null 2>&1 || die S3_IMAGE_UNAVAILABLE "explicit S3 image digest is unavailable locally"
}

preflight() {
  CURRENT_STAGE="PREFLIGHT"
  validate_attempt; validate_tools; validate_graph_manifest; validate_bundle; validate_ports; validate_urls; validate_files_and_relations
  emit_status PREFLIGHT PREFLIGHT_OK "all explicit task-only inputs and live readiness checks passed; no service started"
}

start_all() {
  preflight
  die START_NOT_IMPLEMENTED "R42 service start requires an admitted task-only service graph; no process was started"
}

status_field() {
  local field="$1"
  "$R42_JSON_BIN" - "$STATUS_FILE" "$field" <<'PY'
import json, sys
path, field = sys.argv[1:]
with open(path, encoding="utf-8") as handle:
    value = json.load(handle)
if not isinstance(value, dict) or field not in value or not isinstance(value[field], str):
    raise SystemExit(2)
print(value[field], end="")
PY
}

status_all() {
  CURRENT_STAGE="STATUS"
  validate_attempt
  [[ -f "$STATUS_FILE" && ! -L "$STATUS_FILE" ]] || die STATUS_NOT_STARTED "runner status file does not exist"
  local status code stage timestamp
  if ! status="$(status_field status 2>/dev/null)" || ! code="$(status_field code 2>/dev/null)" \
    || ! stage="$(status_field stage 2>/dev/null)" || ! timestamp="$(status_field timestamp 2>/dev/null)"; then
    die STATUS_FILE_INVALID "runner status file is not a strict JSON status record"
  fi
  case "$status" in PREFLIGHT|BLOCKED|READY|CLEANED) ;; *) die STATUS_VALUE_INVALID "runner status value is not admitted";; esac
  [[ "$code" =~ ^[A-Z0-9_]+$ && "$stage" =~ ^[A-Z0-9_]+$ && "$timestamp" =~ ^[0-9TZ:.-]+$ ]] \
    || die STATUS_FIELDS_INVALID "runner status fields are malformed"
  case "$status" in
    PREFLIGHT)
      printf 'R42_GRAPH status=PREFLIGHT code=%s stage=%s timestamp=%s message=no service started\n' "$code" "$stage" "$timestamp" >&2
      return 78
      ;;
    BLOCKED)
      printf 'R42_GRAPH status=BLOCKED code=%s stage=%s timestamp=%s message=previous runner attempt is blocked\n' "$code" "$stage" "$timestamp" >&2
      return 78
      ;;
    CLEANED)
      printf 'R42_GRAPH status=CLEANED code=%s stage=%s timestamp=%s message=task-owned resources are cleaned\n' "$code" "$stage" "$timestamp"
      return 0
      ;;
    READY)
      validate_tools; validate_graph_manifest; validate_bundle; validate_ports; validate_urls; validate_files_and_relations
      printf 'R42_GRAPH status=READY code=%s stage=%s timestamp=%s message=live task-only readiness checks passed\n' "$code" "$stage" "$timestamp"
      return 0
      ;;
  esac
}

cleanup() {
  CURRENT_STAGE="CLEANUP"
  validate_attempt
  local name pid file; for name in hall gateway media gifting social account version referral inventory billing s3 db; do file="$R42_PID_ROOT/$name.pid"; [[ -f "$file" ]] || continue; pid="$(<"$file")"; [[ "$pid" =~ ^[0-9]+$ ]] && kill -TERM -- "-$pid" 2>/dev/null || true; done
  if [[ -n "${R42_DOCKER_BIN:-}" && -x "${R42_DOCKER_BIN:-}" ]]; then
    for name in "${R42_S3_CONTAINER_NAME:-}" "${R42_DB_CONTAINER_NAME:-}"; do [[ -n "$name" ]] && "$R42_DOCKER_BIN" rm -f "$name" >/dev/null 2>&1 || true; done
  fi
  printf '{"status":"CLEANED","processGroups":"requested","database":"requested","objectStore":"requested"}\n' > "$R42_STATE_ROOT/cleanup-receipt.json"
}

main() {
  local action="${1:-}" config="${2:-}"
  [[ "$action" == preflight || "$action" == start || "$action" == status || "$action" == cleanup ]] || { usage; exit 64; }
  [[ -n "$config" && "$config" == "$REQUIRED_PREFIX"*/* ]] || die CONFIG_PATH_NOT_ISOLATED "config must be inside an R42 /tmp attempt"
  CONFIG_PATH="$config"; load_config; require_var R42_JSON_BIN; [[ "${R42_CONFIG_PATH:-}" == "$CONFIG_PATH" ]] || die CONFIG_PATH_MISMATCH "R42_CONFIG_PATH must equal argument"
  case "$action" in preflight) preflight;; start) start_all;; status) status_all;; cleanup) cleanup;; esac
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  main "$@"
fi
