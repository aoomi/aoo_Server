#!/usr/bin/env bash
set -euo pipefail
if [[ $# -ne 2 ]]; then echo "usage: $0 <java-pid> <output-dir>" >&2; exit 2; fi
pid="$1"; out="$2"; mkdir -p "$out"
jcmd "$pid" GC.run
jcmd "$pid" GC.heap_dump "$out/before.hprof"
echo "Run the configured create-room, complete-game, dissolve loop now, then press Enter." >&2
read -r _
jcmd "$pid" GC.run
jcmd "$pid" GC.heap_dump "$out/after.hprof"
jcmd "$pid" GC.class_histogram > "$out/after-histogram.txt"
printf '{"pid":%s,"before":"before.hprof","after":"after.hprof","histogram":"after-histogram.txt"}\n' "$pid" > "$out/manifest.json"
