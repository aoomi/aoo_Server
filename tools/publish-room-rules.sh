#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-}"
shift || true
ENV_FILE="$ROOT/work/local-runtime/local-dev.env"
SQL_FILE="$(mktemp -t aoo-pdk-room-rules.XXXXXX.sql)"
LOCK_DIR="$ROOT/work/local-runtime/room-rules-publish.lock"
trap 'rm -f "$SQL_FILE"; rmdir "$LOCK_DIR" 2>/dev/null || true' EXIT

acquire_lock() {
  mkdir -p "$(dirname "$LOCK_DIR")"
  mkdir "$LOCK_DIR" 2>/dev/null || { echo '房间规则发布正在执行，拒绝并发发布' >&2; exit 75; }
}

run_mysql() {
  if [[ "$MODE" == local ]]; then
    [[ -f "$ENV_FILE" ]] || { echo "缺少 $ENV_FILE" >&2; exit 2; }
    set -a; source "$ENV_FILE"; set +a
    local container="${AOO_LOCAL_MYSQL_CONTAINER:-aoo-mysql}" database="${AOO_LOCAL_MYSQL_DATABASE:-aoo_login_local}" user="${AOO_LOCAL_MYSQL_USER:-root}"
    docker exec -i -e MYSQL_PWD="${AOO_LOCAL_MYSQL_PASSWORD:?local MySQL password required}" "$container" mysql --default-character-set=utf8mb4 -u"$user" "$database" < "$SQL_FILE"
  else
    MYSQL_PWD="${AOO_RULE_DB_PASSWORD:?AOO_RULE_DB_PASSWORD required}" mysql --default-character-set=utf8mb4 -h "${AOO_RULE_DB_HOST:?AOO_RULE_DB_HOST required}" -P "${AOO_RULE_DB_PORT:-3306}" -u "${AOO_RULE_DB_USER:?AOO_RULE_DB_USER required}" "${AOO_RULE_DB_NAME:?AOO_RULE_DB_NAME required}" < "$SQL_FILE"
  fi
}

case "$MODE" in
  check) ruby "$ROOT/tools/room-rules-publisher.rb" validate ;;
  generate) ruby "$ROOT/tools/room-rules-publisher.rb" generate ;;
  preview) ruby "$ROOT/tools/room-rules-publisher.rb" preview ;;
  local)
    acquire_lock
    ruby "$ROOT/tools/room-rules-publisher.rb" validate
    ruby "$ROOT/tools/room-rules-publisher.rb" generate >/dev/null
    ruby "$ROOT/tools/room-rules-publisher.rb" sql > "$SQL_FILE"
    run_mysql
    ;;
  production)
    [[ "${1:-}" == --play-version && -n "${2:-}" ]] || { echo '生产发布必须显式指定 --play-version <版本>' >&2; exit 2; }
    acquire_lock
    ruby "$ROOT/tools/room-rules-publisher.rb" validate
    ruby "$ROOT/tools/room-rules-publisher.rb" generate >/dev/null
    ruby "$ROOT/tools/room-rules-publisher.rb" sql "$2" > "$SQL_FILE"; run_mysql ;;
  rollback)
    [[ "${1:-}" =~ ^[0-9]+$ ]] || { echo '用法: publish-room-rules.sh rollback RELEASE_ID' >&2; exit 2; }
    ruby "$ROOT/tools/room-rules-publisher.rb" rollback-sql "$1" > "$SQL_FILE"; run_mysql ;;
  *) echo '用法: publish-room-rules.sh {preview|check|generate|local|production --play-version 1.0.0|rollback RELEASE_ID}' >&2; exit 2 ;;
esac
