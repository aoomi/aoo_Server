#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
RUNTIME="$ROOT/work/local-runtime/core-services"
ENV_FILE="$ROOT/work/local-runtime/local-dev.env"
mkdir -p "$RUNTIME" "$ROOT/logs/local-dev"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "缺少 $ENV_FILE；请从 config/local-dev.env.example 创建并填写本机数据库密码" >&2
  exit 2
fi
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

MYSQL_CONTAINER="${AOO_LOCAL_MYSQL_CONTAINER:-aoo-mysql}"
MYSQL_HOST="${AOO_LOCAL_MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${AOO_LOCAL_MYSQL_PORT:-3306}"
MYSQL_DATABASE="${AOO_LOCAL_MYSQL_DATABASE:-aoo_login_local}"
MYSQL_USER="${AOO_LOCAL_MYSQL_USER:-root}"
MYSQL_PASSWORD="${AOO_LOCAL_MYSQL_PASSWORD:?local MySQL password required}"
DB_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"

port_open(){ nc -z 127.0.0.1 "$1" >/dev/null 2>&1; }
wait_port(){ local name="$1" port="$2" deadline=$((SECONDS+30)); while ! port_open "$port"; do (( SECONDS < deadline )) || { echo "$name 启动超时，日志：$ROOT/logs/local-dev/$name.log" >&2; return 1; }; sleep .25; done; }
pid_alive(){ [[ -f "$RUNTIME/$1.pid" ]] && kill -0 "$(cat "$RUNTIME/$1.pid")" 2>/dev/null; }

start_rule_watcher(){
  local name="room-rules" runner="$RUNTIME/room-rules-run.sh" label="com.aoo.bcg.local.room-rules" domain="gui/$(id -u)"
  if pid_alive "$name"; then return; fi
  # launchd 会在 watcher 异常退出后生成新 PID；由 runner 在每次启动时写回，
  # 避免健康检查长期读取第一次启动留下的陈旧 PID。
  printf '#!/usr/bin/env bash\nexport LANG=${LANG:-en_US.UTF-8}\nexport LC_ALL=${LC_ALL:-en_US.UTF-8}\nexport LC_CTYPE=${LC_CTYPE:-en_US.UTF-8}\necho $$ > %q\nexec ruby %q\n' "$RUNTIME/$name.pid" "$ROOT/tools/watch-room-rules.rb" > "$runner"
  chmod 700 "$runner"
  if command -v launchctl >/dev/null 2>&1; then
    local plist="$RUNTIME/room-rules.plist"
    cat > "$plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>Label</key><string>$label</string>
<key>ProgramArguments</key><array><string>$runner</string></array>
<key>WorkingDirectory</key><string>$ROOT</string>
<key>RunAtLoad</key><true/><key>KeepAlive</key><true/><key>ProcessType</key><string>Background</string>
<key>StandardOutPath</key><string>$ROOT/logs/local-dev/room-rules.log</string>
<key>StandardErrorPath</key><string>$ROOT/logs/local-dev/room-rules.log</string>
</dict></plist>
PLIST
    chmod 600 "$plist"
    launchctl bootout "$domain/$label" >/dev/null 2>&1 || true
    if launchctl bootstrap "$domain" "$plist"; then
      launchctl print "$domain/$label" | awk '/pid =/{print $3; exit}' > "$RUNTIME/$name.pid" || true
      if pid_alive "$name"; then return; fi
    fi
    if pid_alive "$name"; then return; fi
    echo "room-rules launchctl 启动失败，降级为 nohup 后台进程；日志仍写入 logs/local-dev/room-rules.log" >&2
  fi
  nohup "$runner" >"$ROOT/logs/local-dev/room-rules.log" 2>&1 < /dev/null &
  echo $! > "$RUNTIME/$name.pid"
}

mysql_exec(){
  docker exec -i -e MYSQL_PWD="$MYSQL_PASSWORD" "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 -u"$MYSQL_USER" "$@"
}

ensure_database(){
  command -v docker >/dev/null || { echo '缺少 Docker，无法启动正式本地 MySQL' >&2; exit 3; }
  if ! docker inspect "$MYSQL_CONTAINER" >/dev/null 2>&1; then echo "缺少 MySQL 容器 $MYSQL_CONTAINER" >&2; exit 3; fi
  if [[ "$(docker inspect -f '{{.State.Running}}' "$MYSQL_CONTAINER")" != true ]]; then docker start "$MYSQL_CONTAINER" >/dev/null; fi
  wait_port mysql "$MYSQL_PORT"
  mysql_exec -e "CREATE DATABASE IF NOT EXISTS \`$MYSQL_DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci"
  AOO_MIGRATION_MYSQL_HOST="$MYSQL_HOST" AOO_MIGRATION_MYSQL_PORT="$MYSQL_PORT" \
    AOO_MIGRATION_MYSQL_USER="$MYSQL_USER" AOO_MIGRATION_MYSQL_PASSWORD="$MYSQL_PASSWORD" \
    AOO_MIGRATION_DATABASE="$MYSQL_DATABASE" "$ROOT/tools/apply_migrations.sh"
  mysql_exec "$MYSQL_DATABASE" < "$ROOT/database/local/seed_local_runtime.sql"
  "$ROOT/tools/publish-room-rules.sh" local
}

build_runtime(){
  if [[ -z "${AOO_JAVA_HOME:-}" ]] && [[ -x /usr/libexec/java_home ]]; then
    AOO_JAVA_HOME="$(/usr/libexec/java_home -v 25)"
    export AOO_JAVA_HOME
  fi
  source "$ROOT/tools/runtime-java26.sh"
  "$ROOT/mvnw" -q -Dmaven.test.skip=true -Dexec.skip=true -pl server/Bootstrap,server/Gateway -am package
}

stage_runtime_classpath(){
  local module="$1" name="$2" source_cp source_jar source_lib snapshot
  source_cp="$(runtime_classpath "$module")"
  source_jar="${source_cp%%:*}"
  source_lib="$ROOT/$module/target/runtime/lib"
  snapshot="$RUNTIME/classpath/${name}-$(date +%Y%m%d%H%M%S)-$$"
  mkdir -p "$snapshot/lib"
  cp "$source_jar" "$snapshot/app.jar"
  cp "$source_lib"/*.jar "$snapshot/lib/"
  printf '%s:%s/*' "$snapshot/app.jar" "$snapshot/lib"
}

start_java(){
  local name="$1" port="$2" classpath="$3" main="$4"; shift 4
  if pid_alive "$name" && port_open "$port"; then return; fi
  if port_open "$port"; then echo "端口 $port 已被非 Aoo $name 进程占用" >&2; exit 4; fi
  local runner="$RUNTIME/$name-run.sh" label="com.aoo.bcg.local.$name" domain="gui/$(id -u)"
  {
    echo '#!/usr/bin/env bash'
    printf 'exec env'
    for item in "$@"; do printf ' %q' "$item"; done
    printf ' %q -Xms64m -Xmx256m -cp %q %q %q\n' "$JAVA_HOME/bin/java" "$classpath" "$main" "$name"
  } > "$runner"
  chmod 700 "$runner"
  if command -v launchctl >/dev/null 2>&1; then
    local plist="$RUNTIME/$name.plist"
    cat > "$plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>Label</key><string>$label</string>
<key>ProgramArguments</key><array><string>$runner</string></array>
<key>WorkingDirectory</key><string>$ROOT</string>
<key>RunAtLoad</key><true/><key>ProcessType</key><string>Background</string>
<key>StandardOutPath</key><string>$ROOT/logs/local-dev/$name.log</string>
<key>StandardErrorPath</key><string>$ROOT/logs/local-dev/$name.log</string>
</dict></plist>
PLIST
    chmod 600 "$plist"
    launchctl bootout "$domain/$label" >/dev/null 2>&1 || true
    launchctl bootstrap "$domain" "$plist"
  else
    nohup "$runner" >"$ROOT/logs/local-dev/$name.log" 2>&1 < /dev/null &
  fi
  wait_port "$name" "$port"
  if command -v launchctl >/dev/null 2>&1; then launchctl print "$domain/$label" | awk '/pid =/{print $3; exit}' > "$RUNTIME/$name.pid"; else pgrep -f "$main.*$name" | head -1 > "$RUNTIME/$name.pid"; fi
}

health(){
  local failed=0
  for spec in 'gateway:8080' 'version:8095' 'account:8096' 'hall:8093' 'social:8097' 'gifting:8101'; do IFS=: read -r name port <<<"$spec"; if pid_alive "$name" && port_open "$port"; then printf 'OK   %-8s pid=%s port=%s\n' "$name" "$(cat "$RUNTIME/$name.pid")" "$port"; else printf 'FAIL %-8s port=%s\n' "$name" "$port"; failed=1; fi; done
  if pid_alive room-rules; then printf 'OK   %-8s pid=%s\n' room-rules "$(cat "$RUNTIME/room-rules.pid")"; else printf 'FAIL %-8s\n' room-rules; failed=1; fi
  if pid_alive gateway && port_open 18080; then printf 'OK   %-8s pid=%s port=%s\n' authority "$(cat "$RUNTIME/gateway.pid")" 18080; else printf 'FAIL %-8s port=%s\n' authority 18080; failed=1; fi
  curl -fsS http://127.0.0.1:8095/health/version >/dev/null || failed=1
  curl -fsS http://127.0.0.1:8096/health/account >/dev/null || failed=1
  curl -fsS http://127.0.0.1:8093/health/hall >/dev/null || failed=1
  curl -fsS http://127.0.0.1:8097/health/social >/dev/null || failed=1
  curl -fsS http://127.0.0.1:8101/health/gifting >/dev/null || failed=1
  return "$failed"
}

stop_all(){
  if command -v launchctl >/dev/null 2>&1; then for name in gateway account version hall social gifting room-rules; do launchctl bootout "gui/$(id -u)/com.aoo.bcg.local.$name" >/dev/null 2>&1 || true; done; fi
  for name in gateway account version hall social gifting room-rules; do if pid_alive "$name"; then kill "$(cat "$RUNTIME/$name.pid")" 2>/dev/null || true; fi; done
  for name in gateway account version hall social gifting room-rules; do if [[ -f "$RUNTIME/$name.pid" ]]; then pid="$(cat "$RUNTIME/$name.pid")"; for _ in {1..40}; do kill -0 "$pid" 2>/dev/null || break; sleep .1; done; kill -9 "$pid" 2>/dev/null || true; rm -f "$RUNTIME/$name.pid"; fi; done
}

stop_one(){
  local name="$1" domain="gui/$(id -u)" label="com.aoo.bcg.local.$1"
  if command -v launchctl >/dev/null 2>&1; then launchctl bootout "$domain/$label" >/dev/null 2>&1 || true; fi
  if pid_alive "$name"; then kill "$(cat "$RUNTIME/$name.pid")" 2>/dev/null || true; fi
  if [[ -f "$RUNTIME/$name.pid" ]]; then
    local pid; pid="$(cat "$RUNTIME/$name.pid")"
    for _ in {1..40}; do kill -0 "$pid" 2>/dev/null || break; sleep .1; done
    kill -9 "$pid" 2>/dev/null || true
    rm -f "$RUNTIME/$name.pid"
  fi
}

start_gateway(){
  local gateway_cp="$1"
  local origins="$AOO_LOCAL_ORIGINS" lan_ip
  lan_ip="$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || true)"
  if [[ -n "$lan_ip" ]]; then
    for preview_port in 7456 7457 7458 7459 7460 5173 5188; do origins+=",http://${lan_ip}:${preview_port}"; done
  fi
  start_java gateway 8080 "$gateway_cp" com.aoo.bcg.gateway.GatewayApplication \
    JAVA_TOOL_OPTIONS=-Dio.netty.eventLoopThreads=4 \
    GATEWAY_DATABASE_URL="$DB_URL" GATEWAY_DATABASE_USER="$MYSQL_USER" GATEWAY_DATABASE_PASSWORD="$MYSQL_PASSWORD" \
    GATEWAY_OFFICIAL_ORIGINS="$origins" GATEWAY_ALLOW_INSECURE_LAN_PREVIEW=true GATEWAY_VERSION_URL=http://127.0.0.1:8095 GATEWAY_ACCOUNT_URL=http://127.0.0.1:8096 GATEWAY_HALL_URL=http://127.0.0.1:8093 HALL_INTERNAL_URL=http://127.0.0.1:8093 HALL_INTERNAL_TOKEN="${AOO_LOCAL_HALL_INTERNAL_TOKEN:-local-hall-internal-token-at-least-32-bytes}" GATEWAY_SOCIAL_URL=http://127.0.0.1:8097 GATEWAY_INTERNAL_TOKEN="${AOO_LOCAL_ROOM_AUTHORITY_TOKEN:-local-room-authority-token-at-least-32-bytes}" GATEWAY_INTERNAL_PORT=18080 GATEWAY_HTTP_PORT=8080
}

start_hall(){
  local hall_cp="$1"
  start_java hall 8093 "$hall_cp" com.aoo.bcg.bootstrap.BootstrapAPP \
    HALL_DATABASE_URL="$DB_URL" HALL_DATABASE_USER="$MYSQL_USER" HALL_DATABASE_PASSWORD="$MYSQL_PASSWORD" \
    HALL_AUTH_SECRET="${AOO_LOCAL_HALL_AUTH_SECRET:-local-hall-auth-secret-at-least-32-bytes}" \
    HALL_INTERNAL_TOKEN="${AOO_LOCAL_HALL_INTERNAL_TOKEN:-local-hall-internal-token-at-least-32-bytes}" HALL_HTTP_PORT=8093 \
    INVITE_SIGNING_KEY="${AOO_LOCAL_INVITE_SIGNING_KEY:-local-invite-signing-key-32-bytes}" INVITE_REFERRAL_URL=http://127.0.0.1:8096 \
    INVITE_REFERRAL_TOKEN="${AOO_LOCAL_INVITE_REFERRAL_TOKEN:-local-referral-token-at-least-32-bytes}" HALL_ROOM_AUTHORITY_URL=http://127.0.0.1:18080 HALL_ROOM_AUTHORITY_TOKEN="${AOO_LOCAL_ROOM_AUTHORITY_TOKEN:-local-room-authority-token-at-least-32-bytes}"
}

start_account(){
  local account_cp="$1"
  start_java account 8096 "$account_cp" com.aoo.bcg.bootstrap.BootstrapAPP \
    ACCOUNT_DATABASE_URL="$DB_URL" ACCOUNT_DATABASE_USER="$MYSQL_USER" ACCOUNT_DATABASE_PASSWORD="$MYSQL_PASSWORD" \
    ACCOUNT_OPERATOR_TOKEN="$AOO_LOCAL_ACCOUNT_OPERATOR_TOKEN" \
    ACCOUNT_IDENTITY_SECOND_FACTOR_TOKEN="${AOO_LOCAL_ACCOUNT_IDENTITY_SECOND_FACTOR_TOKEN:-local-account-second-factor-token-at-least-32-bytes}" \
    ACCOUNT_REGISTRATION_EXPOSE_CODE=true \
    ACCOUNT_GATEWAY_INTERNAL_URL=http://127.0.0.1:18080 ACCOUNT_GATEWAY_INTERNAL_TOKEN="${AOO_LOCAL_ROOM_AUTHORITY_TOKEN:-local-room-authority-token-at-least-32-bytes}" ACCOUNT_HTTP_PORT=8096
}

start_all(){
  ensure_database
  build_runtime
  local bootstrap_cp gateway_cp
  # Snapshot the runtime outside target/. Parallel clean builds must not break
  # already-running services or ServiceLoader's lazy descriptor reads.
  bootstrap_cp="$(stage_runtime_classpath server/Bootstrap bootstrap)"
  # Gateway's production runtime SPI and packaged GameProviders are assembled by Bootstrap.
  gateway_cp="$bootstrap_cp"
  start_java version 8095 "$bootstrap_cp" com.aoo.bcg.bootstrap.BootstrapAPP \
    VERSION_DATABASE_URL="$DB_URL" VERSION_DATABASE_USER="$MYSQL_USER" VERSION_DATABASE_PASSWORD="$MYSQL_PASSWORD" \
    VERSION_INTERNAL_TOKEN="$AOO_LOCAL_VERSION_INTERNAL_TOKEN" VERSION_CLIENT_TOKEN="$AOO_LOCAL_VERSION_CLIENT_TOKEN" VERSION_HTTP_PORT=8095
  start_account "$bootstrap_cp"
  start_hall "$bootstrap_cp"
  start_java social 8097 "$bootstrap_cp" com.aoo.bcg.bootstrap.BootstrapAPP \
    SOCIAL_DATABASE_URL="$DB_URL" SOCIAL_DATABASE_USER="$MYSQL_USER" SOCIAL_DATABASE_PASSWORD="$MYSQL_PASSWORD" SOCIAL_HTTP_PORT=8097
  start_java gifting 8101 "$bootstrap_cp" com.aoo.bcg.bootstrap.BootstrapAPP \
    GIFTING_DATABASE_URL="$DB_URL" GIFTING_DATABASE_USER="$MYSQL_USER" GIFTING_DATABASE_PASSWORD="$MYSQL_PASSWORD" \
    BILLING_INTERNAL_TOKEN="${AOO_LOCAL_BILLING_INTERNAL_TOKEN:-local-billing-internal-token-at-least-32-bytes}" \
    INVENTORY_AUTH_TOKEN="${AOO_LOCAL_INVENTORY_AUTH_TOKEN:-local-inventory-auth-token-at-least-32-bytes}" GIFTING_HTTP_PORT=8101
  start_gateway "$gateway_cp"
  start_rule_watcher
  health
  echo 'Aoo 正式本地核心服务已启动。停止命令：./tools/local-dev-services.sh stop'
}

case "${1:-start}:${2:-all}" in
  start:all) start_all ;;
  restart:all) stop_all; start_all ;;
  stop:all) stop_all ;;
  status:all|health:all) health ;;
  start:gateway) ensure_database; build_runtime; start_gateway "$(stage_runtime_classpath server/Bootstrap bootstrap)"; health ;;
  restart:gateway) build_runtime; gateway_cp="$(stage_runtime_classpath server/Bootstrap bootstrap)"; stop_one gateway; start_gateway "$gateway_cp"; health ;;
  start:account) build_runtime; start_account "$(stage_runtime_classpath server/Bootstrap bootstrap)"; health ;;
  restart:account) build_runtime; account_cp="$(stage_runtime_classpath server/Bootstrap bootstrap)"; stop_one account; start_account "$account_cp"; health ;;
  start:hall) build_runtime; start_hall "$(stage_runtime_classpath server/Bootstrap bootstrap)"; health ;;
  restart:hall) build_runtime; hall_cp="$(stage_runtime_classpath server/Bootstrap bootstrap)"; stop_one hall; start_hall "$hall_cp"; health ;;
  start:gifting) build_runtime; gifting_cp="$(stage_runtime_classpath server/Bootstrap bootstrap)"; start_java gifting 8101 "$gifting_cp" com.aoo.bcg.bootstrap.BootstrapAPP GIFTING_DATABASE_URL="$DB_URL" GIFTING_DATABASE_USER="$MYSQL_USER" GIFTING_DATABASE_PASSWORD="$MYSQL_PASSWORD" BILLING_INTERNAL_TOKEN="${AOO_LOCAL_BILLING_INTERNAL_TOKEN:-local-billing-internal-token-at-least-32-bytes}" INVENTORY_AUTH_TOKEN="${AOO_LOCAL_INVENTORY_AUTH_TOKEN:-local-inventory-auth-token-at-least-32-bytes}" GIFTING_HTTP_PORT=8101 ;;
  restart:gifting) build_runtime; gifting_cp="$(stage_runtime_classpath server/Bootstrap bootstrap)"; stop_one gifting; start_java gifting 8101 "$gifting_cp" com.aoo.bcg.bootstrap.BootstrapAPP GIFTING_DATABASE_URL="$DB_URL" GIFTING_DATABASE_USER="$MYSQL_USER" GIFTING_DATABASE_PASSWORD="$MYSQL_PASSWORD" BILLING_INTERNAL_TOKEN="${AOO_LOCAL_BILLING_INTERNAL_TOKEN:-local-billing-internal-token-at-least-32-bytes}" INVENTORY_AUTH_TOKEN="${AOO_LOCAL_INVENTORY_AUTH_TOKEN:-local-inventory-auth-token-at-least-32-bytes}" GIFTING_HTTP_PORT=8101 ;;
  stop:gateway) stop_one gateway ;;
  *) echo '用法：local-dev-services.sh {start|restart|stop|status} [gateway]' >&2; exit 2 ;;
esac
