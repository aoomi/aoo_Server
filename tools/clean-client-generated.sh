#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
CLIENT=$(CDPATH= cd -- "$ROOT/../Client" && pwd)
mode=${1:---dry-run}
case "$mode" in --dry-run|--apply) ;; *) echo "usage: $0 [--dry-run|--apply]" >&2; exit 2;; esac
if pgrep -f 'CocosCreator|Cocos Creator' >/dev/null 2>&1; then echo 'refusing cleanup while Cocos Creator is running' >&2; exit 3; fi
for name in build library temp; do
  path="$CLIENT/$name"
  [ -e "$path" ] || continue
  case "$path" in "$CLIENT"/build|"$CLIENT"/library|"$CLIENT"/temp) ;; *) echo "unsafe path: $path" >&2; exit 4;; esac
  if [ "$mode" = --dry-run ]; then find "$path" -type f | wc -l | awk -v n="$name" '{print n ": " $1 " files"}'; else rm -rf -- "$path"; fi
done
