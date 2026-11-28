#!/bin/bash
set -euo pipefail
umask 077

ROOT="/Users/aoo/Code/Game/数据库/Aoo-local-databases-20261126T200052Z"
PRIMARY="aoo-export-verify-20261126t200052z-primary-r2"
AUX="aoo-export-verify-20261126t200052z-aux-r2"
ISO_LOBBY="aoo-export-verify-20261126t200052z-iso-lobby-r2"
ISO_SOURCE="aoo-export-verify-20261126t200052z-iso-source-r2"
VERIFY_TMP="$ROOT/restore/RESTORE_VERIFY.tsv.tmp"
KEY_TMP="$ROOT/restore/KEY_ROW_COUNTS.tsv.tmp"
INST_TMP="$ROOT/restore/RESTORE_INSTANCES.tsv.tmp"

for container in "$PRIMARY" "$AUX" "$ISO_LOBBY" "$ISO_SOURCE"; do
  if docker container inspect "$container" >/dev/null 2>&1; then
    echo "NO_OVERWRITE_CONTAINER $container" >&2
    exit 81
  fi
done

printf 'class\tsource_container\trestore_container\tschema\tsource_base_tables\trestore_base_tables\tsource_views\trestore_views\tsource_routines\trestore_routines\tsource_triggers\trestore_triggers\tsource_events\trestore_events\tstatus\n' > "$VERIFY_TMP"
printf 'class\tcontainer\tschema\ttable_name\texact_rows\n' > "$KEY_TMP"
printf 'class\tcontainer\timage\tcontainer_id\tserver_uuid\tversion\tstatus\n' > "$INST_TMP"

docker run -d --network none -e MYSQL_ALLOW_EMPTY_PASSWORD=yes --name "$PRIMARY" mysql:8.0.46 >/dev/null
docker run -d --network none -e MYSQL_ALLOW_EMPTY_PASSWORD=yes --name "$AUX" mysql:8.0.46 >/dev/null
docker run -d --network none -e MYSQL_ALLOW_EMPTY_PASSWORD=yes --name "$ISO_LOBBY" mysql:8.4.5 >/dev/null
docker run -d --network none -e MYSQL_ALLOW_EMPTY_PASSWORD=yes --name "$ISO_SOURCE" mysql:8.4.5 >/dev/null

for container in "$PRIMARY" "$AUX" "$ISO_LOBBY" "$ISO_SOURCE"; do
  ready=0
  for attempt in $(seq 1 90); do
    # The official image briefly exposes an initialization server on port 0.
    # Require the final server (port 3306) twice so imports cannot race the
    # initialization shutdown.
    if [ "$(docker exec "$container" mysql -uroot -N -B -e 'SELECT @@port;' 2>/dev/null || true)" = 3306 ]; then
      sleep 2
      if [ "$(docker exec "$container" mysql -uroot -N -B -e 'SELECT @@port;' 2>/dev/null || true)" = 3306 ]; then
        ready=1
        break
      fi
    fi
    sleep 1
  done
  [ "$ready" = 1 ] || { echo "MYSQL_NOT_READY $container" >&2; exit 82; }
done

restore_one() {
  local class="$1" source_container="$2" target="$3" schema="$4" archive="$5"
  gzip -dc "$ROOT/$archive" | docker exec -i "$target" mysql -uroot
  local before_line sb sv sr st se rb rv rr rt re
  before_line=$(awk -F '\t' -v c="$source_container" -v s="$schema" '$1=="before" && $2==c && $3==s {print; exit}' "$ROOT/OBJECT_COUNTS.tsv")
  [ -n "$before_line" ] || { echo "MISSING_SOURCE_COUNTS $source_container $schema" >&2; exit 83; }
  sb=$(printf '%s\n' "$before_line" | awk -F '\t' '{print $4}')
  sv=$(printf '%s\n' "$before_line" | awk -F '\t' '{print $5}')
  sr=$(printf '%s\n' "$before_line" | awk -F '\t' '{print $6}')
  st=$(printf '%s\n' "$before_line" | awk -F '\t' '{print $7}')
  se=$(printf '%s\n' "$before_line" | awk -F '\t' '{print $8}')
  read -r rb rv rr rt re < <(docker exec "$target" mysql -uroot -N -B -e "SELECT SUM(TABLE_TYPE='BASE TABLE'),SUM(TABLE_TYPE='VIEW'),(SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema='$schema'),(SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_schema='$schema'),(SELECT COUNT(*) FROM information_schema.events WHERE event_schema='$schema') FROM information_schema.tables WHERE table_schema='$schema';" | tr '\t' ' ')
  status=OK
  [ "$sb" = "$rb" ] && [ "$sv" = "$rv" ] && [ "$sr" = "$rr" ] && [ "$st" = "$rt" ] && [ "$se" = "$re" ] || status=MISMATCH
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$class" "$source_container" "$target" "$schema" "$sb" "$rb" "$sv" "$rv" "$sr" "$rr" "$st" "$rt" "$se" "$re" "$status" >> "$VERIFY_TMP"
  [ "$status" = OK ] || exit 84
}

while IFS=$'\t' read -r class confidence source_container cid uuid version host_port schema archive started finished bytes sha status; do
  [ "$class" != class ] || continue
  case "$class:$source_container" in
    primary_runtime:aoo-mysql) target="$PRIMARY" ;;
    auxiliary_shared:aoo-mysql) target="$AUX" ;;
    isolated_active:aoo-local-lobby-r1-mysql) target="$ISO_LOBBY" ;;
    isolated_active:aoo-source-clone-v6-r7-mysql) target="$ISO_SOURCE" ;;
    *) echo "UNEXPECTED_MANIFEST_ROW $class $source_container" >&2; exit 85 ;;
  esac
  restore_one "$class" "$source_container" "$target" "$schema" "$archive"
done < "$ROOT/MANIFEST.tsv"

for pair in "primary_runtime:$PRIMARY" "auxiliary_shared:$AUX" "isolated_active:$ISO_LOBBY" "isolated_active:$ISO_SOURCE"; do
  class=${pair%%:*}; container=${pair#*:}
  ident=$(docker exec "$container" mysql -uroot -N -B -e 'SELECT @@server_uuid,@@version;')
  cid=$(docker inspect -f '{{.Id}}' "$container")
  image=$(docker inspect -f '{{.Config.Image}}' "$container")
  printf '%s\t%s\t%s\t%s\t%s\t%s\tRUNNING_VALIDATION\n' "$class" "$container" "$image" "$cid" "${ident%%$'\t'*}" "${ident#*$'\t'}" >> "$INST_TMP"
done

record_key_counts() {
  local class="$1" container="$2" schema="$3"
  docker exec "$container" mysql -uroot -N -B -e "SELECT TABLE_NAME FROM information_schema.tables WHERE table_schema='$schema' AND TABLE_TYPE='BASE TABLE' AND TABLE_NAME IN ('flyway_schema_history','aoo_game_catalog','aoo_game_release','hall_room','hall_room_member','aoo_room','aoo_room_member','aoo_account') ORDER BY TABLE_NAME;" | while IFS= read -r table; do
    rows=$(docker exec "$container" mysql -uroot -N -B -e "SELECT COUNT(*) FROM \`$schema\`.\`$table\`;")
    printf '%s\t%s\t%s\t%s\t%s\n' "$class" "$container" "$schema" "$table" "$rows" >> "$KEY_TMP"
  done
}

record_key_counts primary_runtime "$PRIMARY" aoo_login_local
record_key_counts isolated_active "$ISO_LOBBY" aoo_login_local
record_key_counts isolated_active "$ISO_SOURCE" aoo_login_local

mv "$VERIFY_TMP" "$ROOT/restore/RESTORE_VERIFY.tsv"
mv "$KEY_TMP" "$ROOT/restore/KEY_ROW_COUNTS.tsv"
mv "$INST_TMP" "$ROOT/restore/RESTORE_INSTANCES.tsv"
chmod 600 "$ROOT/restore/RESTORE_VERIFY.tsv" "$ROOT/restore/KEY_ROW_COUNTS.tsv" "$ROOT/restore/RESTORE_INSTANCES.tsv"

for container in "$PRIMARY" "$AUX" "$ISO_LOBBY" "$ISO_SOURCE"; do
  docker stop "$container" >/dev/null
done

