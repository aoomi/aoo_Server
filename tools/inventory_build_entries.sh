#!/bin/sh
set -eu
out=${1:-work/audit/build-entry-inventory.tsv}
printf 'root\ttype\tabsolutePath\n' > "$out"
scan() {
 root=$1
 [ -d "$root" ] || return 0
 find "$root" -maxdepth 8 \( -type d \( -name .git -o -name node_modules -o -name target -o -name build -o -name library -o -name temp -o -name work \) -prune \) -o -type f \( -name pom.xml -o -name build.gradle -o -name build.gradle.kts -o -name settings.gradle -o -name settings.gradle.kts -o -name package.json -o -name pnpm-lock.yaml -o -name package-lock.json -o -name yarn.lock -o -name build.xml -o -name Dockerfile -o -name docker-compose.yml -o -name compose.yml -o -name '*.iml' -o -name '.classpath' -o -name '.project' -o -name project.json \) -print |
 while IFS= read -r file; do
  base=${file##*/}; type=other
  case "$base" in pom.xml) type=maven;; build.gradle|build.gradle.kts|settings.gradle|settings.gradle.kts) type=gradle;; package.json|pnpm-lock.yaml|package-lock.json|yarn.lock) type=node;; build.xml) type=ant;; Dockerfile|docker-compose.yml|compose.yml) type=container;; *.iml|.classpath|.project) type=ide;; project.json) type=cocos;; esac
  printf '%s\t%s\t%s\n' "$root" "$type" "$file"
 done >> "$out"
}
server_root=$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)
aoo_root=$(CDPATH= cd -- "$server_root/.." && pwd)
source_root=${AOO_SOURCE_ROOT:-$(CDPATH= cd -- "$aoo_root/../Test" 2>/dev/null && pwd || true)}
scan "$server_root"
scan "$aoo_root/Client"
scan "$aoo_root/Admin"
scan "$source_root/QH_DFMJ"
scan "$source_root/情怀后端原代码/Server_game_split"
scan "$source_root/新情怀服务端源码"
LC_ALL=C sort -u -o "$out" "$out"
