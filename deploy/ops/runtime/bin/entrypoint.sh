#!/bin/sh
set -eu

: "${AOO_ENVIRONMENT:?AOO_ENVIRONMENT must be set to production}"
: "${AOO_MAIN_CLASS:?AOO_MAIN_CLASS must name the production main class}"
: "${AOO_RUNTIME_CLASSPATH:=/app/classes:/app/lib/*}"

if [ "$AOO_ENVIRONMENT" != production ]; then
  echo "the production image refuses AOO_ENVIRONMENT=$AOO_ENVIRONMENT" >&2
  exit 78
fi

case "$AOO_MAIN_CLASS" in
  *[!A-Za-z0-9_.$]*) echo "invalid AOO_MAIN_CLASS" >&2; exit 64 ;;
esac

exec java ${JAVA_OPTS:-} -cp "$AOO_RUNTIME_CLASSPATH" "$AOO_MAIN_CLASS"
