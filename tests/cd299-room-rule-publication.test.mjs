import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const migration = await readFile(fileURLToPath(new URL('../database/migrations/V20260919_05__publish_cd299_room_rule_schema.sql', import.meta.url)), 'utf8');

test('CD299 publishes non-empty authoritative room-create fields on the active identity', () => {
  assert.match(migration, /630,'CN-51-01','cd299-v1\.0\.0',@cd299_generation/);
  assert.match(migration, /'lifecycle_state'.*'ACTIVE'|'ACTIVE',CURRENT_TIMESTAMP/s);
  assert.match(migration, /'roomRuleSourceHash','f0ab5905c39adf0ed0d9026684b74a7c238f47743ff31d5b8a3d36d7dabf81c7'/);
  for (const key of ['roomDurationMinutes','startPlayers','operationSeconds','standPolicy','mangoFlipMode','mangoRaise','mangoScore','openingBet','restMango','beatMango','everyHandMango','firstRoundCanRest','eachPlayerMustFollow','earthNineKing','fireproofCard','bigHeadKeepsBase']) {
    assert.match(migration, new RegExp(`'key','${key}'`));
  }
  assert.doesNotMatch(migration, /UPDATE\s+.*开房规则表|INSERT\s+INTO\s+.*开房规则表/i);
});
