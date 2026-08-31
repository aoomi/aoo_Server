#!/bin/sh
set -eu
: "${AOO_ROLLBACK_REVISION:?verified Deployment revision is required}"
printf '%s\n' "$AOO_ROLLBACK_REVISION" | grep -Eq '^[1-9][0-9]*$' || { echo "invalid rollback revision" >&2; exit 64; }
for workload in account gateway bootstrap hall njpdk cdxzmj admin; do
  kubectl -n aoo-production rollout undo "deployment/aoo-$workload" --to-revision="$AOO_ROLLBACK_REVISION"
  kubectl -n aoo-production rollout status "deployment/aoo-$workload" --timeout=10m
done
