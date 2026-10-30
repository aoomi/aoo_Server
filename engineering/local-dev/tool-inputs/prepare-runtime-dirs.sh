#!/usr/bin/env bash
set -euo pipefail
root=${1:?runtime root is required}
umask 027
for dir in config secrets backups logs uploads replays temp; do
  install -d -m 0750 "$root/$dir"
done
