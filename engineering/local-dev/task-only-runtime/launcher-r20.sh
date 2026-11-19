#!/usr/bin/env bash
set -Eeuo pipefail
IFS=$'\n\t'
umask 077

# This launcher is intentionally task-only. It never reads or writes the shared
# local launcher, Server/work/local-runtime, product target directories, or a
# shared database. A missing contract is an error; it is never replaced by a
# default, environment inference, gameId inference, or a partial startup.

readonly ACCEPTED_R19_MANIFEST_SHA256="d2856d6cf8b63f595bbc0b43da3a896e408098ea202bc84af5dd496a952097f7"
readonly REQUIRED_MANIFEST_VERSION="r9-v4"
readonly SCRIPT_NAME="launcher-r20.sh"
CONFIG_PATH=""
ATTEMPT_ROOT=""
STATUS_FILE=""
CURRENT_STAGE="NOT_STARTED"

usage() {
  echo "usage: $SCRIPT_NAME {preflight|start|status|stop|cleanup} /tmp/aoo-c3-runtime-matrix-r26-<attempt>/config.env" >&2
}

emit_status() {
  local status="${1:-BLOCKED}" code="${2:-UNKNOWN}" message="${3:-}" now
  now="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  if [[ -n "$STATUS_FILE" ]]; then
    mkdir -p "$(dirname "$STATUS_FILE")" 2>/dev/null || true
    printf '{"status":"%s","code":"%s","stage":"%s","timestamp":"%s","message":"%s"}\n' \
      "$status" "$code" "$CURRENT_STAGE" "$now" "${message//\/\\}" > "$STATUS_FILE" 2>/dev/null || true
  fi
  printf 'R20_LAUNCHER status=%s code=%s stage=%s message=%s\n' "$status" "$code" "$CURRENT_STAGE" "$message" >&2
}

die() {
  local code="$1" message="$2"
  emit_status "BLOCKED" "$code" "$message"
  exit 78
}

require_command() {
  local command_name="$1"
  command -v "$command_name" >/dev/null 2>&1 || die "MISSING_TOOL_${command_name}" "required tool is unavailable: $command_name"
}

require_var() {
  local name="$1"
  [[ -n "${!name:-}" ]] || die "MISSING_INPUT_${name}" "required explicit input is missing: $name"
}

path_under() {
  local path="$1" root="$2" resolved_path resolved_root relative
  [[ "$path" == /* && "$root" == /* ]] || return 1
  case "$path" in
    ..|../*|*/../*|*/..) return 1 ;;
  esac
  case "$root" in
    ..|../*|*/../*|*/..) return 1 ;;
  esac
  resolved_root="$(canonical_existing "$root")" || return 1
  resolved_path="$(canonical_missing "$path")" || return 1
  relative="$(relative_path "$resolved_root" "$resolved_path")" || return 1
  [[ "$relative" != ".." && "$relative" != ../* ]]
}

canonical_path() {
  canonical_missing "$1"
}

canonical_existing() {
  "$R20_PROCESS_CONTROL_BIN" -c 'import os,sys; p=sys.argv[1]; print(os.path.realpath(p) if os.path.exists(p) else "", end="")' "$1" 2>/dev/null
}

canonical_missing() {
  "$R20_PROCESS_CONTROL_BIN" -c 'import os,sys; print(os.path.realpath(sys.argv[1]), end="")' "$1" 2>/dev/null
}

relative_path() {
  "$R20_PROCESS_CONTROL_BIN" -c 'import os,sys; print(os.path.relpath(sys.argv[2], sys.argv[1]), end="")' "$1" "$2" 2>/dev/null
}

validate_existing_canonical_dir() {
  local name="$1" path="$2" resolved
  [[ "$path" == /* ]] || die "${name}_PATH_NOT_ABSOLUTE" "$name must be absolute"
  case "$path" in
    ..|../*|*/../*|*/..) die "${name}_PARENT_COMPONENT_FORBIDDEN" "$name contains .." ;;
  esac
  [[ -d "$path" && ! -L "$path" ]] || die "${name}_NOT_REGULAR_DIRECTORY" "$name must be an existing non-symlink directory"
  resolved="$(canonical_existing "$path")" || die "${name}_REALPATH_FAILED" "$name cannot be canonicalized"
  [[ -n "$resolved" && "$resolved" == /* ]] || die "${name}_REALPATH_FAILED" "$name cannot be canonicalized"
}

reject_shared_path() {
  local path="$1" canonical
  canonical="$(canonical_path "$path")" || die "PATH_CANONICALIZATION_FAILED" "cannot canonicalize path: $path"
  case "$path$canonical" in
    *"/Server/work/local-runtime"*|*"/Server/logs/local-dev"*|*"/Server/server"*|*"/Client"*)
      die "SHARED_OR_PRODUCT_PATH_FORBIDDEN" "path is not task-owned: $path" ;;
  esac
}

load_config() {
  local line key value
  [[ -f "$CONFIG_PATH" && ! -L "$CONFIG_PATH" ]] || die "CONFIG_NOT_REGULAR_FILE" "config must be a regular non-symlink file: $CONFIG_PATH"
  while IFS= read -r line || [[ -n "$line" ]]; do
    [[ -z "$line" || "$line" == \#* ]] && continue
    [[ "$line" == *=* ]] || die "CONFIG_LINE_INVALID" "config lines must be KEY=VALUE assignments"
    key="${line%%=*}"
    value="${line#*=}"
    [[ "$key" =~ ^R20_[A-Z0-9_]+$ ]] || die "CONFIG_KEY_NOT_ALLOWLISTED" "config key is not R20-scoped: $key"
    case "$value" in
      *'$('|*'`'*|*';'*|*'&&'*|*'||'*|*'>'*|*'<'*)
        die "CONFIG_VALUE_NOT_LITERAL" "config values must be literal, not shell code: $key" ;;
    esac
    export "$key=$value"
  done < "$CONFIG_PATH"
}

validate_attempt_roots() {
  require_var R20_ATTEMPT_ROOT
  require_var R20_BUNDLE_ROOT
  require_var R20_LOG_ROOT
  require_var R20_PID_ROOT
  require_var R20_STATE_ROOT
  case "$R20_ATTEMPT_ROOT" in /tmp/aoo-c3-runtime-matrix-r26-*) ;; *) die "ATTEMPT_ROOT_NOT_ISOLATED" "attempt root must be /tmp/aoo-c3-runtime-matrix-r26-*" ;; esac
  [[ "$R20_ATTEMPT_ROOT" != */ ]] || die "ATTEMPT_ROOT_TRAILING_SLASH" "attempt root must be canonical"
  [[ "$R20_BUNDLE_ROOT" != */ && "$R20_LOG_ROOT" != */ && "$R20_PID_ROOT" != */ && "$R20_STATE_ROOT" != */ ]] || die "ROOT_NOT_CANONICAL" "attempt paths must not end with slash"
  require_var R20_REALPATH_BIN
  validate_host_tool R20_REALPATH_BIN "$R20_REALPATH_BIN"
  require_var R20_PROCESS_CONTROL_BIN
  validate_host_tool R20_PROCESS_CONTROL_BIN "$R20_PROCESS_CONTROL_BIN"
  validate_existing_canonical_dir ATTEMPT_ROOT "$R20_ATTEMPT_ROOT"
  [[ -n "$CONFIG_PATH" ]] && path_under "$CONFIG_PATH" "$R20_ATTEMPT_ROOT" || die "CONFIG_OUTSIDE_ATTEMPT" "config must be inside the canonical attempt root"
  path_under "$R20_BUNDLE_ROOT" "$R20_ATTEMPT_ROOT" || die "BUNDLE_ROOT_OUTSIDE_ATTEMPT" "bundle root must be inside attempt root"
  path_under "$R20_LOG_ROOT" "$R20_ATTEMPT_ROOT" || die "LOG_ROOT_OUTSIDE_ATTEMPT" "log root must be inside attempt root"
  path_under "$R20_PID_ROOT" "$R20_ATTEMPT_ROOT" || die "PID_ROOT_OUTSIDE_ATTEMPT" "pid root must be inside attempt root"
  path_under "$R20_STATE_ROOT" "$R20_ATTEMPT_ROOT" || die "STATE_ROOT_OUTSIDE_ATTEMPT" "state root must be inside attempt root"
  reject_shared_path "$R20_ATTEMPT_ROOT"
  reject_shared_path "$R20_BUNDLE_ROOT"
  mkdir -p "$R20_LOG_ROOT" "$R20_PID_ROOT" "$R20_STATE_ROOT"
  validate_existing_canonical_dir LOG_ROOT "$R20_LOG_ROOT"
  validate_existing_canonical_dir PID_ROOT "$R20_PID_ROOT"
  validate_existing_canonical_dir STATE_ROOT "$R20_STATE_ROOT"
  STATUS_FILE="$R20_STATE_ROOT/launcher-status.json"
}

validate_regular_under() {
  local name="$1" path="$2" root="$3"
  [[ -n "$path" ]] || die "MISSING_PATH_${name}" "missing path: $name"
  path_under "$path" "$root" || die "PATH_OUTSIDE_${name}" "$name is outside task-owned root"
  [[ -f "$path" && ! -L "$path" ]] || die "PATH_NOT_REGULAR_${name}" "$name is not a regular non-symlink file"
  [[ -n "$(canonical_existing "$path")" ]] || die "PATH_NOT_CANONICAL_${name}" "$name cannot be canonicalized"
}

validate_executable_under_bundle() {
  local name="$1" path="$2"
  validate_regular_under "$name" "$path" "$R20_BUNDLE_ROOT"
  [[ -x "$path" ]] || die "RUNNER_NOT_EXECUTABLE_${name}" "$name is not executable"
}

validate_hex() {
  local name="$1" value="$2"
  [[ "$value" =~ ^[0-9a-fA-F]{64}$ ]] || die "${name}_SHA_INVALID" "$name must be a 64-character SHA-256"
}

validate_ports() {
  local -a names=(R20_CLIENT_PORT R20_DB_PORT R20_VERSION_PORT R20_ACCOUNT_PORT R20_HALL_PORT R20_SOCIAL_PORT R20_GIFTING_PORT R20_MEDIA_PORT R20_GATEWAY_PORT R20_AUTHORITY_PORT)
  local name port seen="" reserved
  local -a reserved_ports=(3306 27017 7456 7457 7458 7459 7460 7461 7462 5173 5188 8080 8093 8095 8096 8097 8101 8102 18080)
  for name in "${names[@]}"; do
    require_var "$name"
    port="${!name}"
    [[ "$port" =~ ^[0-9]+$ ]] || die "PORT_INVALID_${name}" "$name must be numeric"
    (( port >= 1024 && port <= 65535 )) || die "PORT_OUT_OF_RANGE_${name}" "$name is outside the task port range"
    for reserved in "${reserved_ports[@]}"; do
      [[ "$port" != "$reserved" ]] || die "SHARED_PORT_FORBIDDEN_${name}" "$name uses forbidden shared port $port"
    done
    case " $seen " in *" $port "*) die "DUPLICATE_TASK_PORT" "port $port is assigned more than once" ;; esac
    seen="$seen $port"
  done
}

validate_host_tool() {
  local name="$1" path="$2"
  [[ "$path" == /* && -f "$path" && -x "$path" ]] || die "HOST_TOOL_INVALID_${name}" "$name must be an explicit executable file"
}

validate_urls() {
  local service upper portvar urlvar port url origin expected_origin origin_count=0
  local -a services=(VERSION ACCOUNT HALL SOCIAL GIFTING MEDIA GATEWAY)
  local -a origins=()
  for upper in "${services[@]}"; do
    portvar="R20_${upper}_PORT"
    urlvar="R20_${upper}_READY_URL"
    require_var "$urlvar"
    port="${!portvar}"
    url="${!urlvar}"
    case "$url" in
      "http://127.0.0.1:${port}"|"http://127.0.0.1:${port}"/*) ;;
      *) die "READINESS_URL_NOT_TASK_LOCAL_${upper}" "$urlvar must target 127.0.0.1:$port" ;;
    esac
  done
  require_var R20_OFFICIAL_ORIGINS
  expected_origin="http://127.0.0.1:${R20_CLIENT_PORT}"
  IFS=',' read -r -a origins <<< "$R20_OFFICIAL_ORIGINS"
  for origin in "${origins[@]}"; do
    [[ -n "$origin" && "$origin" == "$expected_origin" ]] || die "OFFICIAL_ORIGINS_NOT_EXACT_SET" "official origins must equal the one task-local origin"
    origin_count=$((origin_count + 1))
  done
  [[ "$origin_count" == "1" ]] || die "OFFICIAL_ORIGINS_NOT_EXACT_SET" "exactly one official origin is required"
}

validate_tokens() {
  local name path
  local -a names=(R20_VERSION_INTERNAL_TOKEN_FILE R20_VERSION_CLIENT_TOKEN_FILE R20_ACCOUNT_OPERATOR_TOKEN_FILE R20_ACCOUNT_2FA_TOKEN_FILE R20_ACCOUNT_GATEWAY_TOKEN_FILE R20_HALL_AUTH_SECRET_FILE R20_HALL_INTERNAL_TOKEN_FILE R20_HALL_AUTHORITY_TOKEN_FILE R20_HALL_INVITE_SIGNING_KEY_FILE R20_HALL_REFERRAL_TOKEN_FILE R20_GATEWAY_INTERNAL_TOKEN_FILE R20_GIFTING_BILLING_TOKEN_FILE R20_GIFTING_INVENTORY_TOKEN_FILE R20_MEDIA_CLEANUP_TOKEN_FILE R20_MEDIA_ROOM_TOKEN_FILE)
  for name in "${names[@]}"; do
    require_var "$name"
    path="${!name}"
    validate_regular_under "$name" "$path" "$R20_ATTEMPT_ROOT"
    [[ -s "$path" ]] || die "TOKEN_FILE_EMPTY_${name}" "$name is empty"
  done
}

hash_file() {
  case "$(basename "$R20_SHA256_BIN")" in
    shasum) "$R20_SHA256_BIN" -a 256 "$1" | awk '{print $1}' ;;
    sha256sum) "$R20_SHA256_BIN" "$1" | awk '{print $1}' ;;
    *) die "HASH_TOOL_NOT_SUPPORTED" "R20_SHA256_BIN must be sha256sum or shasum" ;;
  esac
}

hash_stream() {
  case "$(basename "$R20_SHA256_BIN")" in
    shasum) "$R20_SHA256_BIN" -a 256 | awk '{print $1}' ;;
    sha256sum) "$R20_SHA256_BIN" | awk '{print $1}' ;;
    *) die "HASH_TOOL_NOT_SUPPORTED" "R20_SHA256_BIN must be sha256sum or shasum" ;;
  esac
}

check_hash_list() {
  local list="$1" differences
  case "$(basename "$R20_SHA256_BIN")" in
    sha256sum) (cd "$R20_BUNDLE_ROOT" && "$R20_SHA256_BIN" --check --strict "$list") >/dev/null || die "BUNDLE_HASH_MISMATCH" "bundle SHA-256 list failed" ;;
    shasum) (cd "$R20_BUNDLE_ROOT" && "$R20_SHA256_BIN" -a 256 -c "$list") >/dev/null || die "BUNDLE_HASH_MISMATCH" "bundle SHA-256 list failed" ;;
  esac
  differences="$(comm -3 <(hash_list_relative_files "$list") <(bundle_relative_files "$list"))"
  [[ -z "$differences" ]] || die "BUNDLE_HASH_COVERAGE_INCOMPLETE" "hash manifest must cover every sealed bundle file exactly once"
}

validate_hash_list_paths() {
  local list="$1" line hash path list_relative
  list_relative="${list#"$R20_BUNDLE_ROOT/"}"
  case "$list_relative" in
    ""|/*|..|../*|*/../*|*/..) die "HASH_LIST_PATH_INVALID" "hash list must be inside the bundle" ;;
  esac
  while IFS= read -r path; do
    [[ -z "$path" || "$path" == \#* ]] && continue
    line="$path"
    [[ "$line" =~ ^([0-9a-fA-F]{64})[[:space:]][[:space:]]([^[:space:]].*)$ ]] || die "HASH_LIST_LINE_INVALID" "hash list lines must be sha256sum records"
    hash="${BASH_REMATCH[1]}"
    path="${BASH_REMATCH[2]}"
    [[ "$path" != /* && "$path" != ./* && "$path" != *"../"* && "$path" != *"/.." && "$path" != ".." ]] || die "HASH_LIST_PATH_FORBIDDEN" "hash list contains an absolute, dot, or parent path"
    [[ "$path" != "$list_relative" ]] || die "HASH_LIST_SELF_REFERENCE" "hash list must not hash itself"
    [[ "$hash" =~ ^[0-9a-fA-F]{64}$ ]] || die "HASH_LIST_SHA_INVALID" "hash list contains a non-SHA-256 digest"
  done < "$list"
}

bundle_relative_files() {
  local list="$1" list_relative="${1#"$R20_BUNDLE_ROOT/"}"
  (cd "$R20_BUNDLE_ROOT" && find -P . -type f ! -path "./$list_relative" -print | sed 's#^\./##' | LC_ALL=C sort)
}

hash_list_relative_files() {
  local list="$1"
  awk 'NF >= 2 { line=$0; sub(/^[^[:space:]]+[[:space:]][[:space:]]/, "", line); print line }' "$list" | LC_ALL=C sort
}

validate_service_loader_projection() {
  local jar entry_count service_text service_entries
  local gateway_entries=0 game_provider_entries=0
  local -a jars=()
  while IFS= read -r -d '' jar; do jars+=("$jar"); done < <(
    find -P "$R20_BUNDLE_ROOT" -type f -name '*.jar' -print0 | LC_ALL=C sort -z
  )
  [[ "${#jars[@]}" -gt 0 ]] || die "SEALED_CLASSPATH_JARS_MISSING" "sealed bundle must contain at least one JAR"
  for jar in "${jars[@]}"; do
    for entry in META-INF/services/com.aoo.bcg.gateway.GatewayRuntimeProvider META-INF/services/com.aoo.bcg.gamespi.GameProvider; do
      entry_count="$(unzip -Z1 "$jar" | awk -v entry="$entry" '$0 == entry {count++} END {print count+0}')"
      [[ "$entry_count" -le 1 ]] || die "SERVICE_RESOURCE_DUPLICATE" "JAR contains duplicate service resource: $entry"
      [[ "$entry_count" == "1" ]] || continue
      service_text="$(unzip -p "$jar" "$entry")"
      service_entries="$(printf '%s\n' "$service_text" | sed '/^[[:space:]]*#/d;/^[[:space:]]*$/d' | awk 'NF {count++} END {print count+0}')"
      [[ "$service_entries" -gt 0 ]] || die "SERVICE_RESOURCE_EMPTY" "service resource has no provider: $entry"
      if [[ "$entry" == META-INF/services/com.aoo.bcg.gateway.GatewayRuntimeProvider ]]; then
        gateway_entries=$((gateway_entries + service_entries))
      else
        game_provider_entries=$((game_provider_entries + service_entries))
      fi
    done
  done
  [[ "$gateway_entries" == "1" ]] || die "GATEWAY_PROVIDER_CARDINALITY" "sealed bundle must expose exactly one GatewayRuntimeProvider"
  [[ "$game_provider_entries" -gt 0 ]] || die "GAME_PROVIDER_RESOURCES_MISSING" "sealed bundle must expose GameProvider service resources"
}

validate_manifest_json() {
  "$R20_JSON_BIN" - "$R20_BUNDLE_MANIFEST" "$R20_EXPECTED_MANIFEST_VERSION" <<'PY' || die "MANIFEST_CONTRACT_INVALID" "canonical manifest is not an accepted 32-row r9-v4 manifest"
import json, sys
path, expected = sys.argv[1:]
with open(path, encoding="utf-8") as handle:
    doc = json.load(handle)
if doc.get("manifestKind") != "runtime-metadata-canonical": raise SystemExit(1)
if doc.get("manifestVersion") != expected: raise SystemExit(1)
if doc.get("manifestStatus") != "SIGNED_CANONICAL_INPUT": raise SystemExit(1)
if len(doc.get("rows", [])) != 32: raise SystemExit(1)
required = {"snapshotSchemaVersion", "cardCodecVersion", "eventInterpreterVersion", "protocolVersion", "sourceHash", "releaseId", "indexGeneration"}
if set(doc.get("requiredFields", [])) != required: raise SystemExit(1)
ids = set()
for row in doc["rows"]:
    if row.get("gameId") == 516 or row.get("admission") != "ACCEPTED": raise SystemExit(1)
    if row.get("gameId") in ids or not isinstance(row.get("gameId"), int): raise SystemExit(1)
    ids.add(row["gameId"])
    for key in required:
        value = row.get(key)
        if value in (None, ""): raise SystemExit(1)
    if not isinstance(row["releaseId"], int) or row["releaseId"] <= 0: raise SystemExit(1)
    if not isinstance(row["indexGeneration"], int) or row["indexGeneration"] <= 0: raise SystemExit(1)
    if len(row["sourceHash"]) != 64: raise SystemExit(1)
PY
}

validate_bundle() {
  require_var R20_BUNDLE_MANIFEST
  require_var R20_MANIFEST_SHA256
  require_var R20_EXPECTED_MANIFEST_VERSION
  require_var R20_BOOTSTRAP_JAR
  require_var R20_BOOTSTRAP_MANIFEST_ENTRY
  require_var R20_GATEWAY_JAR
  require_var R20_JAR_SHA256_FILE
  require_var R20_DB_PREIMAGE_FILE
  require_var R20_DB_PREIMAGE_SHA256
  require_var R20_SHA256_BIN
  require_var R20_JSON_BIN
  require_command unzip
  [[ -x "$R20_SHA256_BIN" ]] || die "HASH_TOOL_NOT_EXECUTABLE" "R20_SHA256_BIN is not executable"
  [[ "$(basename "$R20_SHA256_BIN")" == "sha256sum" || "$(basename "$R20_SHA256_BIN")" == "shasum" ]] || die "HASH_TOOL_NOT_SUPPORTED" "unsupported SHA-256 tool"
  [[ -x "$R20_JSON_BIN" ]] || die "JSON_TOOL_NOT_EXECUTABLE" "R20_JSON_BIN is not executable"
  [[ -d "$R20_BUNDLE_ROOT" && ! -L "$R20_BUNDLE_ROOT" ]] || die "BUNDLE_ROOT_NOT_DIRECTORY" "sealed bundle root must be a non-symlink directory"
  [[ -n "$(canonical_existing "$R20_BUNDLE_ROOT")" ]] || die "BUNDLE_ROOT_NOT_CANONICAL" "sealed bundle root must be canonical"
  if find -P "$R20_BUNDLE_ROOT" -type l -print -quit | grep -q .; then
    die "BUNDLE_SYMLINK_FORBIDDEN" "sealed bundle contains a symlink"
  fi
  validate_regular_under R20_BUNDLE_MANIFEST "$R20_BUNDLE_MANIFEST" "$R20_BUNDLE_ROOT"
  validate_regular_under R20_BOOTSTRAP_JAR "$R20_BOOTSTRAP_JAR" "$R20_BUNDLE_ROOT"
  validate_regular_under R20_GATEWAY_JAR "$R20_GATEWAY_JAR" "$R20_BUNDLE_ROOT"
  validate_regular_under R20_JAR_SHA256_FILE "$R20_JAR_SHA256_FILE" "$R20_BUNDLE_ROOT"
  validate_regular_under R20_DB_PREIMAGE_FILE "$R20_DB_PREIMAGE_FILE" "$R20_BUNDLE_ROOT"
  validate_hex R20_MANIFEST_SHA256 "$R20_MANIFEST_SHA256"
  validate_hex R20_DB_PREIMAGE_SHA256 "$R20_DB_PREIMAGE_SHA256"
  [[ "$R20_MANIFEST_SHA256" == "$ACCEPTED_R19_MANIFEST_SHA256" ]] || die "R19_MANIFEST_SHA_MISMATCH" "manifest SHA is not the accepted R19 SHA"
  [[ "$(hash_file "$R20_BUNDLE_MANIFEST")" == "$R20_MANIFEST_SHA256" ]] || die "MANIFEST_SHA_MISMATCH" "manifest bytes do not match declared SHA"
  [[ "$(hash_file "$R20_DB_PREIMAGE_FILE")" == "$R20_DB_PREIMAGE_SHA256" ]] || die "PREIMAGE_SHA_MISMATCH" "database preimage bytes do not match declared SHA"
  [[ "$R20_EXPECTED_MANIFEST_VERSION" == "$REQUIRED_MANIFEST_VERSION" ]] || die "MANIFEST_VERSION_NOT_ACCEPTED" "only R19 r9-v4 is accepted"
  validate_hash_list_paths "$R20_JAR_SHA256_FILE"
  check_hash_list "$R20_JAR_SHA256_FILE"
  validate_service_loader_projection
  validate_manifest_json
}

validate_database_and_tools() {
  local tool
  local -a tools=(R20_JAVA_BIN R20_CURL_BIN R20_NC_BIN R20_DB_RUNNER R20_DB_CLEANUP_RUNNER R20_VERSION_RUNNER R20_ACCOUNT_RUNNER R20_HALL_RUNNER R20_SOCIAL_RUNNER R20_GIFTING_RUNNER R20_MEDIA_RUNNER R20_GATEWAY_RUNNER)
  local -a host_tools=(R20_REALPATH_BIN R20_PS_BIN R20_PROCESS_CONTROL_BIN)
  require_var R20_DB_URL
  require_var R20_DB_HOST
  require_var R20_DB_PORT
  require_var R20_DB_NAME
  require_var R20_DB_USER
  require_var R20_DB_PASSWORD_FILE
  require_var R20_DB_READY_FILE
  [[ "$R20_DB_HOST" == "127.0.0.1" ]] || die "DB_HOST_NOT_TASK_LOCAL" "R20_DB_HOST must be 127.0.0.1"
  case "$R20_DB_URL" in *"127.0.0.1:${R20_DB_PORT}/"*) ;; *) die "DB_URL_NOT_TASK_LOCAL" "R20_DB_URL must name the task-local DB port and schema" ;; esac
  validate_regular_under R20_DB_PASSWORD_FILE "$R20_DB_PASSWORD_FILE" "$R20_ATTEMPT_ROOT"
  path_under "$R20_DB_READY_FILE" "$R20_ATTEMPT_ROOT" || die "DB_READY_FILE_OUTSIDE_ATTEMPT" "R20_DB_READY_FILE must be inside the attempt root"
  [[ "$R20_DB_READY_FILE" != */ && ! -L "$R20_DB_READY_FILE" ]] || die "DB_READY_FILE_INVALID" "R20_DB_READY_FILE must be a non-symlink path"
  [[ -s "$R20_DB_PASSWORD_FILE" ]] || die "DB_PASSWORD_FILE_EMPTY" "database password file is empty"
  for tool in "${tools[@]}"; do
    require_var "$tool"
    validate_executable_under_bundle "$tool" "${!tool}"
  done
  for tool in "${host_tools[@]}"; do
    require_var "$tool"
    validate_host_tool "$tool" "${!tool}"
  done
  require_var R20_JAVA_EXPECTED_MAJOR
  require_var R20_TIMEZONE
  [[ "$R20_JAVA_EXPECTED_MAJOR" == "25" ]] || die "JAVA_MAJOR_CONTRACT_INVALID" "R20 requires explicit Java major 25"
  [[ "$R20_TIMEZONE" == "UTC" ]] || die "TIMEZONE_CONTRACT_INVALID" "R20 requires UTC"
  local java_version
  java_version="$("$R20_JAVA_BIN" -version 2>&1)" || die "JAVA_VERSION_COMMAND_FAILED" "Java version command failed"
  printf '%s\n' "$java_version" | rg -q 'version "25([.\"]|$)' || die "JAVA_MAJOR_MISMATCH" "configured Java binary is not Java 25"
}

validate_runners_and_paths() {
  require_var R20_CONFIG_PATH
  validate_regular_under R20_CONFIG_PATH "$R20_CONFIG_PATH" "$R20_ATTEMPT_ROOT"
  local service runner_var
  local -a services=(DB VERSION ACCOUNT HALL SOCIAL GIFTING MEDIA GATEWAY)
  for service in "${services[@]}"; do
    runner_var="R20_${service}_RUNNER"
    validate_executable_under_bundle "$runner_var" "${!runner_var}"
  done
  validate_tokens
  validate_ports
  validate_urls
  require_var R20_READY_TIMEOUT_SECONDS
  require_var R20_CLEANUP_TIMEOUT_SECONDS
  [[ "$R20_READY_TIMEOUT_SECONDS" =~ ^[0-9]+$ && "$R20_READY_TIMEOUT_SECONDS" -ge 5 && "$R20_READY_TIMEOUT_SECONDS" -le 180 ]] || die "READY_TIMEOUT_INVALID" "readiness timeout must be 5..180 seconds"
  [[ "$R20_CLEANUP_TIMEOUT_SECONDS" =~ ^[0-9]+$ && "$R20_CLEANUP_TIMEOUT_SECONDS" -ge 5 && "$R20_CLEANUP_TIMEOUT_SECONDS" -le 180 ]] || die "CLEANUP_TIMEOUT_INVALID" "cleanup timeout must be 5..180 seconds"
}

preflight() {
  CURRENT_STAGE="PREFLIGHT"
  validate_attempt_roots
  validate_bundle
  validate_database_and_tools
  validate_runners_and_paths
  CURRENT_STAGE="PREFLIGHT_READY"
  emit_status "READY" "PREFLIGHT_OK" "all explicit task-only inputs are present; no service started"
}

port_open() {
  "$R20_NC_BIN" -z 127.0.0.1 "$1" >/dev/null 2>&1
}

wait_port() {
  local port="$1" deadline=$((SECONDS + R20_READY_TIMEOUT_SECONDS))
  while ! port_open "$port"; do
    (( SECONDS < deadline )) || die "SERVICE_PORT_TIMEOUT" "task service did not open port $port"
    sleep 1
  done
}

wait_http() {
  local url="$1" deadline=$((SECONDS + R20_READY_TIMEOUT_SECONDS))
  while ! "$R20_CURL_BIN" --fail --silent --show-error --max-time 3 "$url" >/dev/null 2>&1; do
    (( SECONDS < deadline )) || die "SERVICE_READINESS_TIMEOUT" "task readiness failed: $url"
    sleep 1
  done
}

wait_file() {
  local path="$1" deadline=$((SECONDS + R20_READY_TIMEOUT_SECONDS))
  while [[ ! -s "$path" ]]; do
    (( SECONDS < deadline )) || die "DB_READY_FILE_TIMEOUT" "database runner did not publish its readiness marker"
    sleep 1
  done
}

pid_file() { printf '%s/%s.pid' "$R20_PID_ROOT" "$1"; }
log_file() { printf '%s/%s.log' "$R20_LOG_ROOT" "$1"; }
owner_file() { printf '%s/%s.owner' "$R20_PID_ROOT" "$1"; }

process_group_id() {
  "$R20_PS_BIN" -o pgid= -p "$1" | awk 'NF { print $1; exit }'
}

record_process_owner() {
  local name="$1" pid="$2" runner="$4" pgid
  pgid="$(process_group_id "$pid")"
  [[ "$pgid" =~ ^[0-9]+$ && "$pgid" == "$pid" ]] || die "PROCESS_GROUP_NOT_OWNED_${name}" "runner did not become its own process group: $name"
  printf 'pid=%s\npgid=%s\nrunner=%s\n' "$pid" "$pgid" "$runner" > "$(owner_file "$name")"
}

group_alive() {
  local pgid="$1"
  [[ "$pgid" =~ ^[0-9]+$ ]] && kill -0 -- "-$pgid" 2>/dev/null
}

start_one() {
  local name="$1" runner="$2" port="$3" pid
  [[ ! -f "$(pid_file "$name")" ]] || die "SERVICE_ALREADY_TRACKED" "attempt already tracks service $name"
  if port_open "$port"; then die "TASK_PORT_ALREADY_OPEN" "task port $port is already occupied"; fi
  CURRENT_STAGE="START_$name"
  env TZ=UTC JAVA_TOOL_OPTIONS="-Duser.timezone=UTC" R20_LAUNCHER_SERVICE="$name" R20_LAUNCHER_CONFIG="$R20_CONFIG_PATH" \
    "$R20_PROCESS_CONTROL_BIN" -c 'import os,sys; os.setsid(); os.execv(sys.argv[1], sys.argv[1:])' "$runner" \
    >"$(log_file "$name")" 2>&1 < /dev/null &
  pid=$!
  printf '%s\n' "$pid" > "$(pid_file "$name")"
  record_process_owner "$name" "$pid" runner "$runner"
  sleep 1
  kill -0 "$pid" 2>/dev/null || die "SERVICE_EXITED_EARLY_$name" "service runner exited before readiness: $name"
  wait_port "$port"
}

cleanup_tracked() {
  local name pid file owner pgid deadline cleanup_failed=0 db_cleanup_status="SKIPPED"
  CURRENT_STAGE="CLEANUP"
  for name in gateway hall gifting social media account version db; do
    file="$(pid_file "$name")"
    [[ -f "$file" ]] || continue
    pid="$(cat "$file")"
    owner="$(owner_file "$name")"
    pgid=""
    if [[ -f "$owner" ]]; then pgid="$(awk -F= '$1 == "pgid" {print $2; exit}' "$owner")"; fi
    if [[ "$pid" =~ ^[0-9]+$ && "$pgid" =~ ^[0-9]+$ && "$pgid" == "$pid" ]] && group_alive "$pgid"; then
      kill -TERM -- "-$pgid" 2>/dev/null || true
      deadline=$((SECONDS + R20_CLEANUP_TIMEOUT_SECONDS))
      while group_alive "$pgid" && (( SECONDS < deadline )); do sleep 1; done
      if group_alive "$pgid"; then kill -KILL -- "-$pgid" 2>/dev/null || true; fi
      group_alive "$pgid" && cleanup_failed=1
    elif [[ -f "$owner" ]]; then
      cleanup_failed=1
    fi
    rm -f "$file" "$owner"
  done
  if [[ -n "${R20_DB_CLEANUP_RUNNER:-}" && -x "${R20_DB_CLEANUP_RUNNER:-}" ]] && \
     path_under "$R20_DB_CLEANUP_RUNNER" "$R20_BUNDLE_ROOT" && \
     [[ -f "$R20_DB_CLEANUP_RUNNER" && ! -L "$R20_DB_CLEANUP_RUNNER" ]]; then
    if env TZ=UTC R20_LAUNCHER_CONFIG="$R20_CONFIG_PATH" "$R20_DB_CLEANUP_RUNNER" >"$(log_file db-cleanup)" 2>&1; then
      db_cleanup_status="PASS"
    else
      db_cleanup_status="FAILED"
      cleanup_failed=1
    fi
  elif [[ -n "${R20_DB_CLEANUP_RUNNER:-}" ]]; then
    db_cleanup_status="SKIPPED_INVALID_INPUT"
  fi
  [[ "$cleanup_failed" == "0" ]] || die "CLEANUP_INCOMPLETE" "owned process groups or database cleanup remain incomplete"
  printf '{"status":"CLEANED","timestamp":"%s","processGroups":"verified","databaseCleanup":"%s"}\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$db_cleanup_status" > "$R20_STATE_ROOT/cleanup-receipt.json"
  emit_status "CLEANED" "CLEANUP_OK" "attempt-owned process groups and database cleanup completed"
}

validate_cleanup_inputs() {
  require_var R20_CLEANUP_TIMEOUT_SECONDS
  [[ "$R20_CLEANUP_TIMEOUT_SECONDS" =~ ^[0-9]+$ && "$R20_CLEANUP_TIMEOUT_SECONDS" -ge 5 && "$R20_CLEANUP_TIMEOUT_SECONDS" -le 180 ]] || die "CLEANUP_TIMEOUT_INVALID" "cleanup timeout must be 5..180 seconds"
}

cleanup_action() {
  CURRENT_STAGE="CLEANUP_PREPARE"
  validate_attempt_roots
  validate_cleanup_inputs
  cleanup_tracked
}

start_all() {
  preflight
  CURRENT_STAGE="STARTING_ALL"
  trap 'cleanup_tracked' ERR INT TERM
  start_one db "$R20_DB_RUNNER" "$R20_DB_PORT"
  wait_file "$R20_DB_READY_FILE"
  start_one version "$R20_VERSION_RUNNER" "$R20_VERSION_PORT"
  wait_http "$R20_VERSION_READY_URL"
  start_one account "$R20_ACCOUNT_RUNNER" "$R20_ACCOUNT_PORT"
  wait_http "$R20_ACCOUNT_READY_URL"
  start_one social "$R20_SOCIAL_RUNNER" "$R20_SOCIAL_PORT"
  wait_http "$R20_SOCIAL_READY_URL"
  start_one gifting "$R20_GIFTING_RUNNER" "$R20_GIFTING_PORT"
  wait_http "$R20_GIFTING_READY_URL"
  start_one media "$R20_MEDIA_RUNNER" "$R20_MEDIA_PORT"
  wait_http "$R20_MEDIA_READY_URL"
  start_one gateway "$R20_GATEWAY_RUNNER" "$R20_GATEWAY_PORT"
  wait_port "$R20_AUTHORITY_PORT"
  wait_http "$R20_GATEWAY_READY_URL"
  start_one hall "$R20_HALL_RUNNER" "$R20_HALL_PORT"
  wait_http "$R20_HALL_READY_URL"
  trap - ERR INT TERM
  CURRENT_STAGE="ACTIVE"
  emit_status "READY" "SERVICE_GRAPH_READY" "all task-only services and Gateway authority are ready"
}

status_all() {
  preflight
  local name file pid
  for name in db version account hall social gifting media gateway; do
    file="$(pid_file "$name")"
    if [[ -f "$file" ]]; then
      pid="$(cat "$file")"
      if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then printf '%s pid=%s alive\n' "$name" "$pid"; else printf '%s pid=%s dead\n' "$name" "$pid"; fi
    else
      printf '%s absent\n' "$name"
    fi
  done
}

main() {
  local action="${1:-}" config="${2:-}"
  [[ "$action" == "preflight" || "$action" == "start" || "$action" == "status" || "$action" == "stop" || "$action" == "cleanup" ]] || { usage; exit 64; }
  [[ -n "$config" && "$config" == /tmp/aoo-c3-runtime-matrix-r26-*/* ]] || die "CONFIG_PATH_NOT_ISOLATED" "config must be inside /tmp/aoo-c3-runtime-matrix-r26-*"
  CONFIG_PATH="$config"
  load_config
  [[ "${R20_CONFIG_PATH:-}" == "$CONFIG_PATH" ]] || die "CONFIG_PATH_INPUT_MISMATCH" "R20_CONFIG_PATH must equal the explicit config argument"
  validate_attempt_roots
  case "$action" in
    preflight) preflight ;;
    start) start_all ;;
    status) status_all ;;
    stop|cleanup) cleanup_action ;;
  esac
}

main "$@"
