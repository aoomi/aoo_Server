#!/bin/sh
set -eu

root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
repository="$root/work/audit/cp12-m2"
log="$root/work/audit/cp12-clean-isolated.log"
marker="$root/work/audit/cp12-clean-isolated.marker"
mkdir -p "$repository" "$(dirname "$log")"
touch "$marker"
cd "$root"
JAVA_HOME=${JAVA_HOME:-"$root/../.toolchains/jdk-26.0.2.1.jdk/Contents/Home"} \
  ./mvnw -Dmaven.repo.local="$repository" -Dexec.skip=true clean verify >"$log" 2>&1
ruby scripts/audit-cp12-clean-isolated.rb
