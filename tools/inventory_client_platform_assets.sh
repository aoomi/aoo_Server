#!/bin/sh
set -eu
server_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
root=${1:-$server_root/../Client}; out=${2:-work/audit/client-platform-assets.tsv}
printf 'kind\tabsolutePath\tvalue\n' > "$out"
find "$root/assets" -type f \( -name '*.scene' -o -name '*.prefab' -o -name '*.meta' -o -name '*.ts' \) | awk '/\.scene$/{c["scene"]++}/\.prefab$/{c["prefab"]++}/\.ts$/{c["script"]++}/\.meta$/{c["meta"]++}END{for(k in c)printf "count\t%s\t%d\n",k,c[k]}' >> "$out"
for d in extensions native build-templates settings profiles development/configs; do [ -e "$root/$d" ] && printf 'platform-root\t%s\tpresent\n' "$root/$d" >> "$out"; done
rg -l --glob '*.meta' 'isBundle|bundleName|priority|compressionType' "$root/assets" | while IFS= read -r f; do printf 'bundle-meta\t%s\tdeclared\n' "$f"; done >> "$out" || true
find "$root" -maxdepth 5 -type f \( -name package.json -o -name project.json -o -name '*.config.*' -o -name '*.json' \) \( -path '*/settings/*' -o -path '*/profiles/*' -o -path '*/extensions/*' -o -path '*/development/configs/*' \) -print | while IFS= read -r f; do printf 'config\t%s\tdiscovered\n' "$f"; done >> "$out"
LC_ALL=C sort -u -o "$out" "$out"
