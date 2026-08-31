#!/bin/sh
set -eu
umask 077

: "${MYSQL_HOST:?}" "${MYSQL_PORT:=3306}" "${MYSQL_DATABASE:?}" "${MYSQL_USER:?}"
: "${MYSQL_PASSWORD_FILE:?MYSQL_PASSWORD_FILE must reference an injected secret file}"
: "${BACKUP_DIR:?}" "${BACKUP_ENCRYPTION_KEY_FILE:?}"
test -r "$MYSQL_PASSWORD_FILE" && test -r "$BACKUP_ENCRYPTION_KEY_FILE"
mkdir -p "$BACKUP_DIR"
stamp=$(date -u +%Y%m%dT%H%M%SZ)
target="$BACKUP_DIR/${MYSQL_DATABASE}-${stamp}.sql.gz.enc"
export MYSQL_PWD="$(cat "$MYSQL_PASSWORD_FILE")"
mysqldump --host="$MYSQL_HOST" --port="$MYSQL_PORT" --user="$MYSQL_USER" --single-transaction --routines --events --triggers --set-gtid-purged=OFF "$MYSQL_DATABASE" \
  | gzip -9 \
  | openssl enc -aes-256-cbc -salt -pbkdf2 -pass "file:$BACKUP_ENCRYPTION_KEY_FILE" -out "$target"
sha256sum "$target" > "$target.sha256"
unset MYSQL_PWD
printf '%s\n' "$target"
