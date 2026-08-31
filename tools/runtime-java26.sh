#!/usr/bin/env bash
for aoo_java_option_var in JAVA_TOOL_OPTIONS JDK_JAVA_OPTIONS; do
  aoo_java_option_value="${!aoo_java_option_var:-}"
  if [[ "$aoo_java_option_value" =~ (^|[[:space:]])-(javaagent|agentlib|agentpath): ]]; then
    echo "禁止通过 ${aoo_java_option_var} 注入未登记 Java Agent" >&2
    return 1 2>/dev/null || exit 1
  fi
done
unset aoo_java_option_var aoo_java_option_value
export JDK_JAVA_OPTIONS="${JDK_JAVA_OPTIONS:-} -Djdk.serialFilter=maxdepth=64;maxrefs=100000;maxbytes=16777216"

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_HOME_DIR="${AOO_JAVA_HOME:-$ROOT_DIR/../.toolchains/jdk-26.0.2.1.jdk/Contents/Home}"
if [[ ! -x "$JAVA_HOME_DIR/bin/java" ]]; then
  echo "缺少 Aoo Java 26 运行时：${JAVA_HOME_DIR}；可通过 AOO_JAVA_HOME 指定兼容 JDK" >&2
  return 1 2>/dev/null || exit 1
fi

runtime_classpath() {
  local module="$1"
  local module_target="$ROOT_DIR/$module/target"
  local runtime_lib="$module_target/runtime/lib"
  local project_jar
  project_jar="$(find "$module_target" -maxdepth 1 -type f -name '*.jar' ! -name '*-sources.jar' ! -name '*-javadoc.jar' -print0 | xargs -0 ls -1t 2>/dev/null | head -1)"
  if [[ -z "$project_jar" || ! -d "$runtime_lib" ]]; then
    echo "缺少标准运行产物：${module_target}；请先执行 ./mvnw -DskipTests package" >&2
    return 1
  fi
  printf '%s:%s/*' "$project_jar" "$runtime_lib"
}

export JAVA_HOME="$JAVA_HOME_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
