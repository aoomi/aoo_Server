#!/bin/sh
set -eu
out=${1:-work/audit/external-capabilities.tsv}
printf 'capability\tabsolutePath\tline\tsymbol\n' > "$out"
scan() {
 root=$1; [ -d "$root" ] || return 0
 rg -n -i --glob '!**/target/**' --glob '!**/build/**' --glob '!**/Legacy/**' --glob '*.{java,ts,properties,yml,yaml,json,xml,conf}' 'wechat|weixin|wx\.|location|gps|payment|pay|voice|gcloud|share|push|firebase|jpush|aliyun|tencent|apple|oauth' "$root" |
 while IFS=: read -r file line value; do
  cap=other
  case "$value" in *[Ww][Ee][Cc][Hh][Aa][Tt]*|*[Ww][Ee][Ii][Xx][Ii][Nn]*|*wx.*) cap=wechat;; *[Ll][Oo][Cc][Aa][Tt][Ii][Oo][Nn]*|*[Gg][Pp][Ss]*) cap=location;; *[Pp][Aa][Yy]*) cap=payment;; *[Vv][Oo][Ii][Cc][Ee]*|*[Gg][Cc][Ll][Oo][Uu][Dd]*) cap=voice;; *[Ss][Hh][Aa][Rr][Ee]*) cap=share;; *[Pp][Uu][Ss][Hh]*|*[Ff][Ii][Rr][Ee][Bb][Aa][Ss][Ee]*|*[Jj][Pp][Uu][Ss][Hh]*) cap=push;; esac
  symbol=$(printf '%s' "$value" | sed -E 's/[=:].*$//' | cut -c1-160)
  printf '%s\t%s\t%s\t%s\n' "$cap" "$file" "$line" "$symbol"
 done >> "$out" || true
}
server_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
aoo_root=$(CDPATH= cd -- "$server_root/.." && pwd)
scan "$aoo_root/Client/assets"
scan "$server_root/modules"
scan "$server_root/server/LegacyCommon"
scan "$server_root/server/LegacyAccountServer"
scan "$server_root/server/LegacyGameHall"
LC_ALL=C sort -u -o "$out" "$out"
