#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
CLIENT="$ROOT/../Client"

fail() { printf 'authority entry cleanup gate failed: %s\n' "$1" >&2; exit 1; }

[[ ! -e "$ROOT/server/gameServer/src/core/network/client2game/handler/room/CBaseCreateRoom.java" ]] \
  || fail 'retired CBaseCreateRoom handler is still present'

if rg -n 'createContext\("/(legacy|api/v1/hall)' \
  "$ROOT/server/Hall/src/main" "$ROOT/server/Club/src/main" \
  "$ROOT/server/Billing/src/main" "$ROOT/server/Gifting/src/main" >/dev/null; then
  fail 'a replaced legacy HTTP route is still mounted'
fi

if rg -n 'hall\.game_catalog|CBaseCreateRoom|CClubCreateRoom|legacy-data/|UISelectCity|SelectCity\.prefab' \
  "$CLIENT/assets" "$ROOT/protocol/aoo-protocol-v2.json" >/dev/null; then
  fail 'a retired catalog, room-create, or region-selection runtime entry remains'
fi

node - "$CLIENT/profiles/v2/packages/builder.json" <<'NODE'
const fs = require('fs');
const file = process.argv[2];
const data = JSON.parse(fs.readFileSync(file, 'utf8'));
const tasks = Object.values(data.BuildTaskManager?.taskMap ?? {}).map(entry => entry.options).filter(Boolean);
if (!tasks.length) throw new Error('Creator build task list is empty');
const bootstrap = '8d2a710a-63f6-4186-a17f-81339a64ef2c';
for (const task of tasks) {
  if (task.startScene !== bootstrap) throw new Error(`${task.taskName}: start scene is not Bootstrap`);
  const urls = (task.scenes ?? []).map(scene => String(scene.url));
  if (!urls.includes('db://assets/Games/Common/Scenes/GameRoom2D.scene')) throw new Error(`${task.taskName}: GameRoom2D missing`);
  if (urls.some(url => url.includes('/Native/'))) throw new Error(`${task.taskName}: retired Native path remains`);
}
NODE

printf 'authority entry cleanup gate passed\n'
