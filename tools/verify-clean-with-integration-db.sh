#!/bin/zsh
set -euo pipefail

ROOT="${0:A:h:h}"
LOCAL_ENV="${AOO_IT_ENV_FILE:-${ROOT}/.env.integration.local}"
if [[ -f "$LOCAL_ENV" ]]; then
  mode="$(stat -f '%Lp' "$LOCAL_ENV")"
  (( (8#$mode & 8#077) == 0 )) || { print -u2 "$LOCAL_ENV must be owner-only (chmod 600)"; exit 2; }
  set -a
  source "$LOCAL_ENV"
  set +a
fi

: "${AOO_IT_DB_HOST:=127.0.0.1}"
: "${AOO_IT_DB_PORT:=3306}"
: "${AOO_IT_DB_USER:=root}"
: "${AOO_IT_DB_CONTAINER:=}"
: "${AOO_IT_KEEP_SCHEMA:=false}"
: "${JAVA_HOME:?JAVA_HOME must point to the approved JDK}"

NODE_BIN="${ROOT}/../.toolchains/node-v24.19.0-darwin-arm64/bin"
[[ -x "${NODE_BIN}/node" ]] || { print -u2 "approved Node runtime is missing: ${NODE_BIN}/node"; exit 2; }
export PATH="${NODE_BIN}:${PATH}"

task_id="${AOO_IT_TASK_ID:-$(date -u +%Y%m%d%H%M%S)-$$}"
task_id="${task_id//[^A-Za-z0-9_]/_}"
schema="aoo_it_${task_id}"
[[ "$schema" == aoo_it_* && ${#schema} -le 64 ]] || { print -u2 'unsafe integration schema name'; exit 2; }

db_password="${AOO_IT_DB_PASSWORD:-}"
if [[ -n "$AOO_IT_DB_CONTAINER" ]]; then
  command -v docker >/dev/null || { print -u2 'docker is required for AOO_IT_DB_CONTAINER'; exit 2; }
  running="$(docker inspect -f '{{.State.Running}}' "$AOO_IT_DB_CONTAINER" 2>/dev/null || true)"
  [[ "$running" == true ]] || { print -u2 "integration DB container is not running: $AOO_IT_DB_CONTAINER"; exit 2; }
  if [[ -z "$db_password" ]]; then
    db_password="$(docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$AOO_IT_DB_CONTAINER" | sed -n 's/^MYSQL_ROOT_PASSWORD=//p')"
  fi
  [[ -n "$db_password" ]] || { print -u2 'integration DB password unavailable'; exit 2; }
  docker exec -e MYSQL_PWD="$db_password" "$AOO_IT_DB_CONTAINER" mysql -uroot -e "CREATE DATABASE $schema CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci" >/dev/null
  drop_schema() {
    [[ "$AOO_IT_KEEP_SCHEMA" == true ]] && return
    docker exec -e MYSQL_PWD="$db_password" "$AOO_IT_DB_CONTAINER" mysql -uroot -e "DROP DATABASE IF EXISTS $schema" >/dev/null 2>&1 || true
  }
else
  : "${db_password:?set AOO_IT_DB_PASSWORD or AOO_IT_DB_CONTAINER in the owner-only local env}"
  command -v mysql >/dev/null || { print -u2 'mysql client is required for external integration DB'; exit 2; }
  MYSQL_PWD="$db_password" mysql -h "$AOO_IT_DB_HOST" -P "$AOO_IT_DB_PORT" -u "$AOO_IT_DB_USER" -e "CREATE DATABASE \`$schema\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
  drop_schema() {
    [[ "$AOO_IT_KEEP_SCHEMA" == true ]] && return
    MYSQL_PWD="$db_password" mysql -h "$AOO_IT_DB_HOST" -P "$AOO_IT_DB_PORT" -u "$AOO_IT_DB_USER" -e "DROP DATABASE IF EXISTS \`$schema\`" >/dev/null 2>&1 || true
  }
fi
trap drop_schema EXIT INT TERM

AOO_MIGRATION_MYSQL_HOST="$AOO_IT_DB_HOST" \
AOO_MIGRATION_MYSQL_PORT="$AOO_IT_DB_PORT" \
AOO_MIGRATION_MYSQL_USER="$AOO_IT_DB_USER" \
AOO_MIGRATION_MYSQL_PASSWORD="$db_password" \
AOO_MIGRATION_DATABASE="$schema" \
  "$ROOT/tools/apply_migrations.sh"

# Required deterministic fixture for concurrency FK coverage; isolated schema only.
fixture="INSERT INTO aoo_game_catalog(game_id,game_code,display_name,category_code,family_code,provider_key,catalog_schema_version,status) VALUES(62,'it_game_62','IT Game 62','LONG_CARD','LONG_CARD_UNCLASSIFIED','it.provider.62',1,'ACTIVE'); INSERT INTO aoo_play_version(game_id,play_version,default_region_code,rule_schema_version,ui_schema_version,component_schema_version,content_hash,status,activated_at,created_by) VALUES(62,'v1','GLOBAL',1,1,1,REPEAT('a',64),'ACTIVE',CURRENT_TIMESTAMP(3),1);"
if [[ -n "$AOO_IT_DB_CONTAINER" ]]; then
  docker exec -e MYSQL_PWD="$db_password" "$AOO_IT_DB_CONTAINER" mysql -uroot -D "$schema" -e "$fixture" >/dev/null
else
  MYSQL_PWD="$db_password" mysql -h "$AOO_IT_DB_HOST" -P "$AOO_IT_DB_PORT" -u "$AOO_IT_DB_USER" "$schema" -e "$fixture"
fi

export AOO_DB_IT_URL="jdbc:mysql://${AOO_IT_DB_HOST}:${AOO_IT_DB_PORT}/${schema}?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=UTC"
export AOO_DB_IT_USER="$AOO_IT_DB_USER"
export AOO_DB_IT_PASSWORD="$db_password"

cd "$ROOT"
"$ROOT/tools/verify-region-classification-boundary.sh"
"$ROOT/tools/verify-authority-entry-cleanup.sh"
./mvnw clean verify
print "clean integration verify passed with isolated schema: $schema"
