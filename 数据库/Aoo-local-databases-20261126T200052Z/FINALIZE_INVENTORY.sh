#!/bin/bash
set -euo pipefail
umask 077

ROOT="/Users/aoo/Code/Game/数据库/Aoo-local-databases-20261126T200052Z"
INV="$ROOT/INSTANCE_INVENTORY.tsv"
STOPPED="$ROOT/STOPPED_ARTIFACTS.tsv"
IMPORT="$ROOT/IMPORT_ORDER.txt"

printf 'class\tconfidence\tcontainer\tcontainer_id\timage\tstate\tserver_version\thost_port\tschema_count\tnote\n' > "$INV"
awk -F '\t' 'NR>1 {key=$1 FS $3 FS $4 FS $6 FS $7; schemas[key]++} END {for (k in schemas) print k FS schemas[k]}' "$ROOT/MANIFEST.tsv" | sort | while IFS=$'\t' read -r class container cid version host_port schema_count; do
  confidence=$(awk -F '\t' -v c="$container" -v cl="$class" 'NR>1 && $1==cl && $3==c {print $2; exit}' "$ROOT/MANIFEST.tsv")
  image=$(docker inspect -f '{{.Config.Image}}' "$container")
  state=$(docker inspect -f '{{.State.Status}}' "$container")
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$class" "$confidence" "$container" "$cid" "$image" "$state" "$version" "$host_port" "$schema_count" exported >> "$INV"
done

printf 'class\tcontainer\tcontainer_id\timage\tstate\tfinished_utc\tdata_mount_type\tdata_mount_name_or_path\testimated_size\taction\n' > "$STOPPED"
docker ps -a --filter status=exited --format '{{.Names}}' | sort | while IFS= read -r container; do
  row=$(docker inspect "$container" | jq -r '.[0] | select((.Name|test("^/aoo-")) and ((.Config.Image|test("mysql";"i")) or (.Name|test("(mysql|db)";"i")))) | [.Name,.Id,.Config.Image,.State.Status,.State.FinishedAt,([.Mounts[]? | select(.Destination=="/var/lib/mysql") | .Type][0] // "UNKNOWN"),([.Mounts[]? | select(.Destination=="/var/lib/mysql") | (.Name // .Source)][0] // "UNKNOWN")] | @tsv')
  [ -n "$row" ] || continue
  name=$(printf '%s\n' "$row" | cut -f1 | sed 's#^/##')
  cid=$(printf '%s\n' "$row" | cut -f2)
  image=$(printf '%s\n' "$row" | cut -f3)
  state=$(printf '%s\n' "$row" | cut -f4)
  finished=$(printf '%s\n' "$row" | cut -f5)
  mount_type=$(printf '%s\n' "$row" | cut -f6)
  mount_name=$(printf '%s\n' "$row" | cut -f7)
  size=UNKNOWN
  if [ "$mount_type" = volume ]; then
    size=$(docker system df -v | awk -v v="$mount_name" '$1==v {print $3; exit}')
    size=${size:-UNKNOWN}
  elif [ "$mount_type" = bind ] && [ -d "$mount_name" ]; then
    size=$(du -sh "$mount_name" 2>/dev/null | awk '{print $1}')
    size=${size:-UNKNOWN}
  fi
  printf 'stopped_artifact\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\tINVENTORY_ONLY_NOT_STARTED_OR_MOUNTED\n' "$name" "$cid" "$image" "$state" "$finished" "$mount_type" "$mount_name" "$size" >> "$STOPPED"
done

cat > "$IMPORT" <<'EOF'
Restore into four separate fresh MySQL instances; do not merge classes.
1. PRIMARY_RUNTIME (MySQL 8.0.46): dumps/primary_runtime__aoo-mysql__aoo_login_local.full.sql.gz
2. AUXILIARY_SHARED (MySQL 8.0.46): all dumps/auxiliary_shared__aoo-mysql__*.full.sql.gz, one schema per archive, in MANIFEST.tsv order
3. ISOLATED_ACTIVE lobby (MySQL 8.4.5): dumps/isolated_active__aoo-local-lobby-r1-mysql__aoo_login_local.full.sql.gz
4. ISOLATED_ACTIVE source clone (MySQL 8.4.5): dumps/isolated_active__aoo-source-clone-v6-r7-mysql__aoo_login_local.full.sql.gz
For each archive: gzip -t first, verify CHECKSUMS.sha256, then gzip -dc ARCHIVE | mysql -u ADMIN.
STOPPED_ARTIFACTS.tsv is inventory only and has no dump in this package.
EOF

chmod 600 "$INV" "$STOPPED" "$IMPORT"
