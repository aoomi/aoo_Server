#!/bin/sh
set -eu
default_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
root=${1:-$default_root}; out=${2:-work/audit/server-runtime-inventory.tsv}
printf 'kind\tabsolutePath\tline\tvalue\n' > "$out"
rg -n --glob '!**/target/**' --glob '!**/build/**' --glob '!work/**' --glob '*.java' 'public static void main\s*\(' "$root" | while IFS=: read -r file line value; do printf 'main\t%s\t%s\t%s\n' "$file" "$line" "$value"; done >> "$out" || true
rg -n --glob '!**/target/**' --glob '!**/build/**' --glob '!work/**' --glob '*.properties' --glob '*.yml' --glob '*.yaml' --glob '*.json' --glob '*.xml' --glob '*.conf' 'server\.port|listenPort|httpPort|wsPort|port.*[:=]' "$root" | while IFS=: read -r file line value; do printf 'port-config\t%s\t%s\t%s\n' "$file" "$line" "$value"; done >> "$out" || true
find "$root/server" -mindepth 2 -maxdepth 4 -name pom.xml -type f -print | while IFS= read -r f; do printf 'maven-module\t%s\t0\tpom.xml\n' "$f"; done >> "$out"
find "$root/tools" -maxdepth 1 -type f \( -name 'start-*-local.sh' -o -name 'stop-*-local.sh' -o -name '*health*.sh' \) -print | while IFS= read -r f; do printf 'lifecycle-script\t%s\t0\t%s\n' "$f" "${f##*/}"; done >> "$out"
LC_ALL=C sort -u -o "$out" "$out"
