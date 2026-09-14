#!/bin/zsh
set -eu

ROOT="${0:A:h:h}"
USER_NAME="${AOO_MIGRATION_MYSQL_USER:-root}"
PASSWORD="${AOO_MIGRATION_MYSQL_PASSWORD:?set AOO_MIGRATION_MYSQL_PASSWORD}"
DATABASE="${AOO_MIGRATION_DATABASE:?set AOO_MIGRATION_DATABASE}"
HOST="${AOO_MIGRATION_MYSQL_HOST:-127.0.0.1}"
PORT="${AOO_MIGRATION_MYSQL_PORT:-3306}"
[[ "$DATABASE" == aoo_* ]] || { print -u2 'migration database must use aoo_ prefix'; exit 2; }

export FLYWAY_URL="jdbc:mysql://${HOST}:${PORT}/${DATABASE}?useUnicode=true&characterEncoding=UTF-8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
export FLYWAY_USER="$USER_NAME"
export FLYWAY_PASSWORD="$PASSWORD"
export FLYWAY_LOCATIONS="${AOO_MIGRATION_LOCATIONS:-filesystem:${ROOT}/database/migrations}"
export FLYWAY_TABLE="flyway_schema_history"
export FLYWAY_ENCODING="UTF-8"
export FLYWAY_VALIDATE_MIGRATION_NAMING="true"
export FLYWAY_VALIDATE_ON_MIGRATE="true"
export FLYWAY_OUT_OF_ORDER="true"
export FLYWAY_CLEAN_DISABLED="true"
export FLYWAY_BASELINE_ON_MIGRATE="false"
export FLYWAY_INIT_SQL="SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci; SET SESSION time_zone = '+00:00'; SET SESSION sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION'"

"$ROOT/mvnw" -N -q org.flywaydb:flyway-maven-plugin:13.3.0:"${AOO_MIGRATION_COMMAND:-migrate}"
print 'flyway-migration-apply: passed'
