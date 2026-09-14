#!/bin/zsh
set -eu

ROOT="${0:A:h:h}"
MYSQL_CONTAINER="${F03_MYSQL_CONTAINER:-aoo-mysql}"
REDIS_CONTAINER="${F03_REDIS_CONTAINER:-aoo-redis}"
MQ_CONTAINER="${F03_MQ_CONTAINER:-aoo-mqbroker}"
JAVA_HOME="${JAVA_HOME:-$ROOT/../.toolchains/jdk-26.0.2.1.jdk/Contents/Home}"
[[ "${AOO_ENVIRONMENT:-}" == test ]] || { print -u2 'fault injection is restricted to AOO_ENVIRONMENT=test'; exit 2; }
[[ "${F03_CONFIRM_FAULT_INJECTION:-}" == YES ]] || { print -u2 'set F03_CONFIRM_FAULT_INJECTION=YES'; exit 2; }
OUTAGE_SECONDS="${F03_MAX_OUTAGE_SECONDS:-20}"
(( OUTAGE_SECONDS >= 1 && OUTAGE_SECONDS <= 60 )) || { print -u2 'F03_MAX_OUTAGE_SECONDS must be 1..60'; exit 2; }
for container in "$MYSQL_CONTAINER" "$REDIS_CONTAINER" "$MQ_CONTAINER"; do
  [[ "$container" == aoo-* ]] || { print -u2 "non-isolated container rejected: $container"; exit 2; }
done

: "${AOO_DB_IT_URL:?set AOO_DB_IT_URL}"
: "${AOO_DB_IT_USER:?set AOO_DB_IT_USER}"
: "${AOO_DB_IT_PASSWORD:?set AOO_DB_IT_PASSWORD}"
: "${AOO_MQ_IT_NAMESRV:?set AOO_MQ_IT_NAMESRV}"
: "${AOO_MQ_IT_DB_URL:?set AOO_MQ_IT_DB_URL}"
: "${AOO_MQ_IT_DB_USER:?set AOO_MQ_IT_DB_USER}"
: "${AOO_MQ_IT_DB_PASSWORD:?set AOO_MQ_IT_DB_PASSWORD}"

export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
cd "$ROOT"
PAUSED=()

cleanup() {
  for container in $PAUSED; do docker unpause "$container" >/dev/null 2>&1 || true; done
}
trap cleanup EXIT INT TERM

pause_container() {
  docker pause "$1" >/dev/null
  PAUSED+=("$1")
}

resume_container() {
  docker unpause "$1" >/dev/null
  PAUSED=(${PAUSED:#$1})
}

expect_failure() {
  # A dependency outage must fail within a bounded period; hanging is itself a
  # failed resilience result. Perl's alarm is available on the supported hosts.
  if perl -e '$timeout=shift; alarm $timeout; exec @ARGV' "$OUTAGE_SECONDS" "$@" >/dev/null 2>&1; then
    print -u2 "expected dependency failure but command succeeded: $*"
    return 1
  fi
}

db_test=(./mvnw -q -pl server/Billing -am -Dtest=JdbcBillingIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test)
mq_test=(./mvnw -q -pl server/GameCommon -am -Dtest=RocketMqOutboxIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test)

print '[1/3] MySQL baseline, interruption and recovery'
$db_test
pause_container "$MYSQL_CONTAINER"
expect_failure $db_test
resume_container "$MYSQL_CONTAINER"
$db_test

print '[2/3] RocketMQ baseline, interruption and recovery'
$mq_test
pause_container "$MQ_CONTAINER"
expect_failure $mq_test
resume_container "$MQ_CONTAINER"
$mq_test

print '[3/3] Redis interruption and protocol recovery'
python3 tools/f03_local_protocol_smoke.py >/dev/null
pause_container "$REDIS_CONTAINER"
expect_failure python3 tools/f03_local_protocol_smoke.py
resume_container "$REDIS_CONTAINER"
python3 tools/f03_local_protocol_smoke.py >/dev/null

print 'F03 local dependency fault-injection passed'
