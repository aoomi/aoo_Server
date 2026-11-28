#!/bin/bash
set -euo pipefail
umask 077

ROOT="/Users/aoo/Code/Game/数据库/Aoo-local-databases-20261126T200052Z"
MANIFEST="$ROOT/MANIFEST.tsv.tmp"
OBJECTS="$ROOT/OBJECT_COUNTS.tsv.tmp"
DEFINERS="$ROOT/DEFINERS.tsv.tmp"
GUARDS="$ROOT/metadata/GUARDS.tsv.tmp"

mysqlq() {
  local container="$1"; shift
  printf '%s\n' "$*" | docker exec -i "$container" sh -lc 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -B'
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
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$phase" "$now" "$container" "$cid" "$uuid" "$version" "$locks" "$trx" "$active" "$ddl" "${history:-UNKNOWN}" "$free" >> "$GUARDS"
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
    FROM information_schema.tables WHERE table_schema='$schema';" >> "$OBJECTS"
}

definers() {
  local container="$1" schema="$2"
  mysqlq "$container" "
    SELECT '$container','$schema','VIEW',TABLE_NAME,DEFINER FROM information_schema.views WHERE table_schema='$schema'
    UNION ALL SELECT '$container','$schema','TRIGGER',TRIGGER_NAME,DEFINER FROM information_schema.triggers WHERE trigger_schema='$schema'
    UNION ALL SELECT '$container','$schema',ROUTINE_TYPE,ROUTINE_NAME,DEFINER FROM information_schema.routines WHERE routine_schema='$schema'
    UNION ALL SELECT '$container','$schema','EVENT',EVENT_NAME,DEFINER FROM information_schema.events WHERE event_schema='$schema';" >> "$DEFINERS"
}

dump_one() {
  local class="$1" confidence="$2" container="$3" host_port="$4" schema="$5"
  local safe archive tmp started finished bytes sha cid ident uuid version
  safe=$(printf '%s' "$schema" | tr -c 'A-Za-z0-9_.-' '_')
  archive="$ROOT/dumps/${class}__${container}__${safe}.full.sql.gz"
  tmp="$archive.tmp"
  [ ! -e "$archive" ] && [ ! -e "$tmp" ] || { echo "NO_OVERWRITE $archive" >&2; exit 71; }
  guard before_resume "$container" "$schema"
  object_counts before "$container" "$schema"
  definers "$container" "$schema"
  started=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  docker exec "$container" sh -lc 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --quick --skip-lock-tables --routines --events --triggers --hex-blob --set-gtid-purged=OFF --default-character-set=utf8mb4 --no-tablespaces --databases "$1"' sh "$schema" | gzip -9 > "$tmp"
  test -s "$tmp"
  gzip -t "$tmp"
  chmod 600 "$tmp"
  guard after_resume "$container" "$schema"
  object_counts after "$container" "$schema"
  mv "$tmp" "$archive"
  finished=$(date -u +%Y-%m-%dT%H:%M:%SZ)
  bytes=$(stat -f %z "$archive")
  sha=$(shasum -a 256 "$archive" | awk '{print $1}')
  cid=$(docker inspect -f '{{.Id}}' "$container")
  ident=$(mysqlq "$container" 'SELECT @@server_uuid,@@version;')
  uuid=${ident%%$'\t'*}; version=${ident#*$'\t'}
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\tOK\n' "$class" "$confidence" "$container" "$cid" "$uuid" "$version" "$host_port" "$schema" "${archive#$ROOT/}" "$started" "$finished" "$bytes" "$sha" >> "$MANIFEST"
}

dump_one auxiliary_shared MEDIUM aoo-mysql 127.0.0.1:3306 aoo_v4_shadow_20261127
dump_one isolated_active HIGH aoo-local-lobby-r1-mysql 127.0.0.1:13306 aoo_login_local
dump_one isolated_active HIGH aoo-source-clone-v6-r7-mysql 127.0.0.1:13328 aoo_login_local

for base in MANIFEST OBJECT_COUNTS DEFINERS; do
  mv "$ROOT/$base.tsv.tmp" "$ROOT/$base.tsv"
  chmod 600 "$ROOT/$base.tsv"
done
mv "$ROOT/metadata/GUARDS.tsv.tmp" "$ROOT/metadata/GUARDS.tsv"
chmod 600 "$ROOT/metadata/GUARDS.tsv"
chmod 600 "$ROOT/EXPORT_RUNBOOK.sh" "$ROOT/RESUME_RUNBOOK.sh"
