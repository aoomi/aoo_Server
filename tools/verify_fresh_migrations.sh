#!/bin/zsh
set -eu

ROOT="${0:A:h:h}"
CONTAINER="${AOO_MIGRATION_MYSQL_CONTAINER:-aoo-mysql}"
USER_NAME="${AOO_MIGRATION_MYSQL_USER:-root}"
PASSWORD="${AOO_MIGRATION_MYSQL_PASSWORD:?set AOO_MIGRATION_MYSQL_PASSWORD}"
DATABASE="${AOO_MIGRATION_VERIFY_DATABASE:-aoo_fresh_verify}"
REPORT="$ROOT/docs/generated/tool12-migration-verification.json"
[[ "$DATABASE" == aoo_*_verify || "$DATABASE" == aoo_fresh_verify ]] || {
  print -u2 "verification database name must be isolated and end in _verify"; exit 2;
}

mysql_exec() { docker exec -e MYSQL_PWD="$PASSWORD" -i "$CONTAINER" mysql -u"$USER_NAME" "$@"; }
apply() {
  AOO_MIGRATION_MYSQL_USER="$USER_NAME" \
  AOO_MIGRATION_MYSQL_PASSWORD="$PASSWORD" \
  AOO_MIGRATION_DATABASE="$DATABASE" \
  AOO_MIGRATION_LOCATIONS="${1:-filesystem:${ROOT}/database/migrations}" \
  AOO_MIGRATION_COMMAND="${2:-migrate}" \
    "$ROOT/tools/apply_migrations.sh" >/dev/null
}
TMP=$(mktemp -d "${TMPDIR:-/tmp}/aoo-migrations.XXXXXX")
trap 'rm -rf "$TMP"' EXIT INT TERM
mysql_exec -e "DROP DATABASE IF EXISTS \`$DATABASE\`; CREATE DATABASE \`$DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

# Empty database and every intermediate version are upgraded by the same
# Flyway engine. Each prefix makes the next invocation a genuine incremental
# upgrade rather than a static SQL parse.
mkdir -p "$TMP/progressive"
MIGRATION_COUNT=0
while IFS= read -r migration; do
  cp "$migration" "$TMP/progressive/"
  apply "filesystem:$TMP/progressive"
  MIGRATION_COUNT=$((MIGRATION_COUNT + 1))
done < <(ruby "$ROOT/tools/list_migrations.rb")

# Re-applying an already current schema must be a no-op and validation must
# cover every installed checksum/version.
apply
apply "filesystem:${ROOT}/database/migrations" validate

# A failed migration must be recorded, repaired explicitly, and then allow a
# clean retry without dropping the database.
mkdir -p "$TMP/failure"
cp "$ROOT"/database/migrations/*.sql "$TMP/failure/"
cat > "$TMP/failure/V20991231_99__intentional_failure.sql" <<'SQL'
THIS IS INTENTIONALLY INVALID SQL;
SQL
if apply "filesystem:$TMP/failure"; then
  print -u2 'intentional migration unexpectedly succeeded'; exit 1
fi
rm "$TMP/failure/V20991231_99__intentional_failure.sql"
apply "filesystem:$TMP/failure" repair
apply "filesystem:$TMP/failure"

TABLES=$(mysql_exec -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DATABASE'")
EXPECTED=$(ruby -e 'tables=ARGV.flat_map{|f|File.read(f).scan(/CREATE TABLE(?: IF NOT EXISTS)?\s+([A-Za-z0-9_]+)/i).flatten}; puts tables.map(&:downcase).uniq.length + 1' "$ROOT"/database/migrations/*.sql)
[[ "$TABLES" -eq "$EXPECTED" ]] || { print -u2 "expected $EXPECTED tables including migration history, found $TABLES"; exit 1; }
MISSING=$(mysql_exec -Nse "SELECT CONCAT(required.table_name,'.',required.column_name) FROM (SELECT 'aoo_room_event' table_name,'visibility' column_name UNION ALL SELECT 'aoo_room_event','owner_player_id' UNION ALL SELECT 'aoo_currency_balance','balance' UNION ALL SELECT 'aoo_club_member','member_status') required LEFT JOIN information_schema.columns actual ON actual.table_schema='$DATABASE' AND actual.table_name=required.table_name AND actual.column_name=required.column_name WHERE actual.column_name IS NULL")
[[ -z "$MISSING" ]] || { print -u2 "missing required columns: $MISSING"; exit 1; }
HISTORY_COUNT=$(mysql_exec -Nse "SELECT COUNT(*) FROM \`$DATABASE\`.flyway_schema_history WHERE success=1 AND type='SQL'")
[[ "$HISTORY_COUNT" -eq "$MIGRATION_COUNT" ]] || { print -u2 "migration history $HISTORY_COUNT != files $MIGRATION_COUNT"; exit 1; }
mkdir -p "${REPORT:h}"
cat > "$REPORT" <<JSON
{
  "schemaVersion": 1,
  "task": "TOOL12",
  "passed": true,
  "database": "$DATABASE",
  "migrationCount": $MIGRATION_COUNT,
  "successfulHistoryRows": $HISTORY_COUNT,
  "tableCount": $TABLES,
  "checks": ["empty database", "every incremental version", "repeat no-op", "checksum validation", "failed migration repair and retry", "history/file parity", "required schema objects"]
}
JSON
print "migration-verification: passed ($MIGRATION_COUNT migrations, $TABLES tables)"
