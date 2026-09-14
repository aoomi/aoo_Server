#!/bin/sh
set -eu
root=$(CDPATH= cd -- "$(dirname "$0")/../../.." && pwd)
manifest="$root/deploy/ops/kubernetes/runtime.yaml"
workflow="$root/.github/workflows/release.yml"
for file in "$manifest" "$workflow" "$root/deploy/ops/Dockerfile"; do test -s "$file"; done
kubectl kustomize "$root/deploy/ops" >"${TMPDIR:-/tmp}/aoo-kustomize-static.yaml"
test "$(grep -c '^kind: Deployment$' "${TMPDIR:-/tmp}/aoo-kustomize-static.yaml")" -eq 7
test "$(grep -c '^kind: StatefulSet$' "${TMPDIR:-/tmp}/aoo-kustomize-static.yaml")" -eq 1
grep -q 'image: mongo:8.0.29-noble' "${TMPDIR:-/tmp}/aoo-kustomize-static.yaml"
! grep -Eq 'image:[[:space:]]*mongo:(8\.0|8\.2|8\.3|latest)([^0-9.]|$)' "${TMPDIR:-/tmp}/aoo-kustomize-static.yaml"
for service in account gateway bootstrap hall njpdk cdxzmj admin; do
  grep -q "name: aoo-$service" "${TMPDIR:-/tmp}/aoo-kustomize-static.yaml"
done
grep -q 'cosign sign-blob' "$workflow"
grep -q 'cosign verify-blob' "$workflow"
! grep -Eq 'REPLACE_WITH_RELEASE_DIGEST|registry\.invalid|run: echo .*Sign' "$manifest" "$workflow"
! grep -REn '(password|token|secret)[[:space:]]*[:=][[:space:]]*[^${<[:space:]][^[:space:]]+' "$root/deploy/ops" --include='*.yaml' --include='*.sh'
python3 -m unittest discover -s "$root/deploy/ops/tests" -v
printf '%s\n' 'production delivery static verification passed; real production acceptance remains deferred'
