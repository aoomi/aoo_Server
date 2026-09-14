#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
: "${JAVA_HOME:=$ROOT/../.toolchains/jdk-26.0.2.1.jdk/Contents/Home}"
export JAVA_HOME PATH="$JAVA_HOME/bin:$PATH"
mkdir -p work/audit
touch work/audit/unused38-clean-start.marker
./mvnw -q clean verify
ruby scripts/audit-unused38-clean-recovery.rb
