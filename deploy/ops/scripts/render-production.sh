#!/bin/sh
set -eu

root=$(CDPATH= cd -- "$(dirname "$0")/../../.." && pwd)
: "${AOO_PUBLIC_HOST:?production DNS host is required}"

image_pattern='^[A-Za-z0-9._:/-][A-Za-z0-9._:/-]*@sha256:[0-9a-f]\{64\}$'
host_pattern='^[a-z0-9][a-z0-9.-]*[a-z0-9]$'
for service in ACCOUNT GATEWAY BOOTSTRAP HALL NJPDK CDXZMJ ADMIN; do
  eval "image=\${AOO_${service}_IMAGE:-}"
  test -n "$image" || { echo "AOO_${service}_IMAGE is required" >&2; exit 64; }
  printf '%s\n' "$image" | grep -q "$image_pattern" || { echo "invalid AOO_${service}_IMAGE" >&2; exit 64; }
done
printf '%s\n' "$AOO_PUBLIC_HOST" | grep -q "$host_pattern" || { echo "invalid AOO_PUBLIC_HOST" >&2; exit 64; }

rendered=$(mktemp "${TMPDIR:-/tmp}/aoo-production.XXXXXX")
trap 'rm -f "$rendered"' EXIT HUP INT TERM
kubectl kustomize "$root/deploy/ops" >"$rendered"
sed \
    -e "s|aoo/account:release|$AOO_ACCOUNT_IMAGE|g" \
    -e "s|aoo/gateway:release|$AOO_GATEWAY_IMAGE|g" \
    -e "s|aoo/bootstrap:release|$AOO_BOOTSTRAP_IMAGE|g" \
    -e "s|aoo/hall:release|$AOO_HALL_IMAGE|g" \
    -e "s|aoo/njpdk:release|$AOO_NJPDK_IMAGE|g" \
    -e "s|aoo/cdxzmj:release|$AOO_CDXZMJ_IMAGE|g" \
    -e "s|aoo/admin:release|$AOO_ADMIN_IMAGE|g" \
    -e "s|account.production.invalid|$AOO_PUBLIC_HOST|g" \
    "$rendered"
