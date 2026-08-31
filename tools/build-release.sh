#!/bin/sh
set -eu

root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$root"
ruby tools/legacy-protocol-ledger/verify_ledger.rb
revision=$(ruby tools/aoo-build-version.rb)
case "$revision" in
  *-SNAPSHOT) echo "release revision must not be a SNAPSHOT: $revision" >&2; exit 1 ;;
esac
export SOURCE_DATE_EPOCH=${SOURCE_DATE_EPOCH:-1787414400}
source_revision=${AOO_SOURCE_REVISION:?AOO_SOURCE_REVISION is required for release provenance}
pipeline_id=${AOO_PIPELINE_ID:?AOO_PIPELINE_ID is required for release provenance}
dependency_lock_sha=$(cat pom.xml .mvn/wrapper/maven-wrapper.properties ../Client/pnpm-lock.yaml | shasum -a 256 | awk '{print $1}')
config_sha=$(find deploy database/migrations protocol -type f -print | LC_ALL=C sort | xargs cat | shasum -a 256 | awk '{print $1}')
exec ./mvnw -Drevision="$revision" -Dproject.build.outputTimestamp="$SOURCE_DATE_EPOCH" -Daoo.source.revision="$source_revision" -Daoo.pipeline.id="$pipeline_id" -Daoo.dependency.lock.sha256="$dependency_lock_sha" -Daoo.config.sha256="$config_sha" "$@"
