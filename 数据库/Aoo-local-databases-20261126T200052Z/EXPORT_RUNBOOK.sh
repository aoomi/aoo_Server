#!/bin/bash
set -euo pipefail
umask 077

ROOT="/Users/aoo/Code/Game/数据库/Aoo-local-databases-20261126T200052Z"
mkdir -p "$ROOT/dumps" "$ROOT/metadata" "$ROOT/restore"
chmod 700 "$ROOT" "$ROOT/dumps" "$ROOT/metadata" "$ROOT/restore"

MANIFEST="$ROOT/MANIFEST.tsv"
OBJECTS="$ROOT/OBJECT_COUNTS.tsv"
DEFINERS="$ROOT/DEFINERS.tsv"
GUARDS="$ROOT/metadata/GUARDS.tsv"
printf 'class\tconfidence\tcontainer\tcontainer_id\tserver_uuid\tversion\thost_port\tschema\tarchive\tstarted_utc\tfinished_utc\tbytes\tsha256\tstatus\n' > "$MANIFEST.tmp"
printf 'phase\tcontainer\tschema\tbase_tables\tviews\troutines\ttriggers\tevents\testimated_rows\tdata_index_bytes\tnon_innodb\n' > "$OBJECTS.tmp"
printf 'container\tschema\tobject_type\tobject_name\tdefiner\n' > "$DEFINERS.tmp"
printf 'phase\tutc\tcontainer\tcontainer_id\tserver_uuid\tversion\tpending_metadata_locks\tlong_transactions\tactive_non_sleep\tactive_ddl_or_migration\thistory_list_length\tfree_kib\n' > "$GUARDS.tmp"

mysqlq() {
  local container="$1"; shift
  printf '%s\n' "$*" | docker exec -i "$container" sh -lc 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -B'
}

identity() {
  local container="$1"
  local cid ident
  cid=$(docker inspect -f '{{.Id}}' "$container")
  ident=$(mysqlq "$container" 'SELECT @@server_uuid,@@version;')
  printf '%s\t%s\n' "$cid" "$ident"
}

guard() {
  local phase="$1" container="$2" schema="$3" now cid ident uuid version locks trx active ddl history free
  now=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  cid=$(docker inspect -f '{{.Id}}' "$container")
  ident=$(mysqlq "$container" 'SELECT @@server_uuid,@@version;')
  uuid=${ident%%$'\t'*}; version=${ident#*$'\t'}
  locks=$(mysqlq "$container" "SELECT COUNT(*) FROM performance_schema.metadata_locks WHERE LOCK_STATUS='PENDING';")
  trx=$(mysqlq "$container" "SELECT COUNT(*) FROM information_schema.innodb_trx WHERE TIMESTAMPDIFF(SECOND,trx_started,NOW())>60;")
  active=$(mysqlq "$container" "SELECT COUNT(*) FROM information_schema.processlist WHERE ID<>CONNECTION_ID() AND COMMAND<>'Sleep' AND USER<>'event_scheduler';")
  ddl=$(mysqlq "$container" "SELECT COUNT(*) FROM information_schema.processlist WHERE ID<>CONNECTION_ID() AND (INFO REGEXP '(^|[[:space:]])(ALTER|CREATE|DROP|RENAME|TRUNCATE)[[:space:]]' OR INFO LIKE '%flyway%');")
  history=$(mysqlq "$container" 'SHOW ENGINE INNODB STATUS;' | sed -n 's/.*History list length \([0-9][0-9]*\).*/\1/p' | head -1)
  free=$(df -Pk "$ROOT" | awk 'NR==2{print $4}')
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$phase" "$now" "$container" "$cid" "$uuid" "$version" "$locks" "$trx" "$active" "$ddl" "${history:-UNKNOWN}" "$free" >> "$GUARDS.tmp"
  if [ "$locks" != 0 ] || [ "$trx" != 0 ] || [ "$active" != 0 ] || [ "$ddl" != 0 ]; then
    echo "GUARD_FAILURE $container $schema locks=$locks trx=$trx active=$active ddl=$ddl" >&2
    exit 70
  fi
}

object_counts() {
  local phase="$1" container="$2" schema="$3"
  mysqlq "$container" "SELECT '$phase','$container','$schema',
    SUM(TABLE_TYPE='BASE TABLE'),SUM(TABLE_TYPE='VIEW'),
    (SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema='$schema'),
    (SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='$schema'),
    (SELECT COUNT(*) FROM information_schema.events WHERE event_schema='$schema'),
    COALESCE(SUM(CASE WHEN TABLE_TYPE='BASE TABLE' THEN TABLE_ROWS ELSE 0 END),0),
    COALESCE(SUM(CASE WHEN TABLE_TYPE='BASE TABLE' THEN DATA_LENGTH+INDEX_LENGTH ELSE 0 END),0),
    SUM(TABLE_TYPE='BASE TABLE' AND ENGINE<>'InnoDB')
    FROM information_schema.tables WHERE table_schema='$schema';" >> "$OBJECTS.tmp"
}

definers() {
  local container="$1" schema="$2"
  mysqlq "$container" "
    SELECT '$container','$schema','VIEW',TABLE_NAME,DEFINER FROM information_schema.views WHERE table_schema='$schema'
    UNION ALL SELECT '$container','$schema','TRIGGER',TRIGGER_NAME,DEFINER FROM information_schema.triggers WHERE trigger_schema='$schema'
    UNION ALL SELECT '$container','$schema',ROUTINE_TYPE,ROUTINE_NAME,DEFINER FROM information_schema.routines WHERE routine_schema='$schema'
    UNION ALL SELECT '$container','$schema','EVENT',EVENT_NAME,DEFINER FROM information_schema.events WHERE event_schema='$schema';" >> "$DEFINERS.tmp"
}

dump_one() {
  local class="$1" confidence="$2" container="$3" host_port="$4" schema="$5"
  local safe archive tmp started finished bytes sha cid ident uuid version
  safe=$(printf '%s' "$schema" | tr -c 'A-Za-z0-9_.-' '_')
  archive="$ROOT/dumps/${class}__${container}__${safe}.full.sql.gz"
  tmp="$archive.tmp"
  [ ! -e "$archive" ] && [ ! -e "$tmp" ] || { echo "NO_OVERWRITE $archive" >&2; exit 71; }
  guard before "$container" "$schema"
  object_counts before "$container" "$schema"
  definers "$container" "$schema"
  started=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  docker exec "$container" sh -lc 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --quick --skip-lock-tables --routines --events --triggers --hex-blob --set-gtid-purged=OFF --default-character-set=utf8mb4 --no-tablespaces --databases "$1"' sh "$schema" | gzip -9 > "$tmp"
  test -s "$tmp"
  gzip -t "$tmp"
  chmod 600 "$tmp"
  guard after "$container" "$schema"
  object_counts after "$container" "$schema"
  mv "$tmp" "$archive"
  finished=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  bytes=$(stat -f %z "$archive")
  sha=$(shasum -a 256 "$archive" | awk '{print $1}')
  cid=$(docker inspect -f '{{.Id}}' "$container")
  ident=$(mysqlq "$container" 'SELECT @@server_uuid,@@version;')
  uuid=${ident%%$'\t'*}; version=${ident#*$'\t'}
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\tOK\n' "$class" "$confidence" "$container" "$cid" "$uuid" "$version" "$host_port" "$schema" "${archive#$ROOT/}" "$started" "$finished" "$bytes" "$sha" >> "$MANIFEST.tmp"
}

dump_one primary_runtime HIGH aoo-mysql 127.0.0.1:3306 aoo_login_local
while IFS= read -r schema; do
  [ -n "$schema" ] && dump_one auxiliary_shared MEDIUM aoo-mysql 127.0.0.1:3306 "$schema"
done <<'SCHEMAS'
aoo_billing_scratch_20261126_1259
aoo_contract_scratch_20261126_1251
aoo_current_contract_empty_20261126
aoo_login_contract_scratch_20261126
aoo_login_fresh_verify
aoo_login_preapply_verify_20261126
aoo_pricing_rebind_scratch_20261126
aoo_pricing_scratch_20261126
aoo_room_arch_authority_scratch_20261126
aoo_room_card_arch_hash_scratch_20261126
aoo_room_card_arch_scratch_20261126
aoo_room_card_r2_rollback_20261126
aoo_room_card_r2_scratch_20261126
aoo_room_route_scratch_20261126
aoo_t36_clone
aoo_t36_fresh
aoo_t36_verify
aoo_t80_fresh
aoo_ui_hash_scratch_20261126
aoo_ui_hash_v06_scratch_20261126
aoo_unique_generation_scratch_20261126
aoo_v4_shadow_20261127
SCHEMAS
dump_one isolated_active HIGH aoo-local-lobby-r1-mysql 127.0.0.1:13306 aoo_login_local
dump_one isolated_active HIGH aoo-source-clone-v6-r7-mysql 127.0.0.1:13328 aoo_login_local

for f in "$MANIFEST" "$OBJECTS" "$DEFINERS" "$GUARDS"; do
  mv "$f.tmp" "$f"
  chmod 600 "$f"
done
chmod 600 "$ROOT/EXPORT_RUNBOOK.sh"
