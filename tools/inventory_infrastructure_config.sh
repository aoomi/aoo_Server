#!/bin/sh
set -eu
default_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
root=${1:-$default_root}; out=${2:-work/audit/infrastructure-config-inventory.tsv}
printf 'facility\tabsolutePath\tline\tkey\n' > "$out"
rg -n -i --glob '!**/target/**' --glob '!**/build/**' --glob '!work/**' --glob '*.{properties,yml,yaml,json,xml,conf,java}' 'jdbc:|redis|rocketmq|kafka|rabbitmq|mongodb|elasticsearch|s3|oss|cos|config.?center|nacos|zookeeper' "$root" |
while IFS=: read -r file line value; do
 facility=other
 case "$value" in *[Jj][Dd][Bb][Cc]*|*[Mm][Yy][Ss][Qq][Ll]*|*[Pp][Oo][Ss][Tt][Gg][Rr][Ee][Ss]*) facility=database;; *[Rr][Ee][Dd][Ii][Ss]*) facility=redis;; *[Rr][Oo][Cc][Kk][Ee][Tt][Mm][Qq]*|*[Kk][Aa][Ff][Kk][Aa]*|*[Rr][Aa][Bb][Bb][Ii][Tt][Mm][Qq]*) facility=mq;; *[Mm][Oo][Nn][Gg][Oo]*) facility=document-store;; *[Ee][Ll][Aa][Ss][Tt][Ii][Cc]*) facility=search;; *[Ss]3*|*[Oo][Ss][Ss]*|*[Cc][Oo][Ss]*) facility=object-store;; *[Nn][Aa][Cc][Oo][Ss]*|*[Zz][Oo][Oo][Kk][Ee][Ee][Pp][Ee][Rr]*|*[Cc][Oo][Nn][Ff][Ii][Gg]*[Cc][Ee][Nn][Tt][Ee][Rr]*) facility=config-center;; esac
 key=$(printf '%s' "$value" | sed -E 's/[=:].*$//' | tr -d '\r' | cut -c1-160)
 printf '%s\t%s\t%s\t%s\n' "$facility" "$file" "$line" "$key"
done >> "$out" || true
LC_ALL=C sort -u -o "$out" "$out"
