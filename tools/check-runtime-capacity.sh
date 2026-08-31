#!/usr/bin/env bash
set -euo pipefail
path=${1:?path required}; requested=${2:-0}; reserve=${3:-268435456}; domain=${4:-UNKNOWN}
available=$(df -Pk "$path" | awk 'NR==2 {print $4 * 1024}')
if (( available < requested || available - requested < reserve )); then
  printf 'insufficient disk capacity domain=%s requested=%s reserve=%s usable=%s\n' "$domain" "$requested" "$reserve" "$available" >&2
  exit 75
fi
