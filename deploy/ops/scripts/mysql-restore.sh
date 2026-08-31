#!/bin/sh
set -eu

: "${MYSQL_HOST:?}" "${MYSQL_PORT:=3306}" "${MYSQL_DATABASE:?}" "${MYSQL_USER:?}"
: "${MYSQL_PASSWORD_FILE:?}" "${BACKUP_ENCRYPTION_KEY_FILE:?}" "${BACKUP_FILE:?}"
test "${ALLOW_RESTORE:-}" = "I_UNDERSTAND_DATA_WILL_CHANGE" || { echo "restore acknowledgement missing" >&2; exit 64; }
test -r "$MYSQL_PASSWORD_FILE" && test -r "$BACKUP_ENCRYPTION_KEY_FILE" && test -r "$BACKUP_FILE"
sha256sum -c "$BACKUP_FILE.sha256"
export MYSQL_PWD="$(cat "$MYSQL_PASSWORD_FILE")"
openssl enc -d -aes-256-cbc -pbkdf2 -pass "file:$BACKUP_ENCRYPTION_KEY_FILE" -in "$BACKUP_FILE" \
  | gzip -dc \
  | mysql --host="$MYSQL_HOST" --port="$MYSQL_PORT" --user="$MYSQL_USER" --database="$MYSQL_DATABASE" --binary-mode
mysql --host="$MYSQL_HOST" --port="$MYSQL_PORT" --user="$MYSQL_USER" --database="$MYSQL_DATABASE" --batch --skip-column-names -e 'SELECT 1'
unset MYSQL_PWD
