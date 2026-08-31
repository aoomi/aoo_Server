import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const outputs = [
  path.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/protocol/GeneratedProtocolIds.java'),
  path.resolve(root, '../Client/assets/Common/Code/Runtime/network/GeneratedProtocolIds.ts'),
  path.join(root, 'docs/generated/protocol-reference.md')
];
const before = new Map(outputs.map(file => [file, fs.existsSync(file) ? fs.readFileSync(file) : null]));
execFileSync(process.execPath, [path.join(root, 'tools/protocol/generate.mjs')], { stdio: 'inherit' });
const stale = outputs.filter(file => before.get(file) === null || !before.get(file).equals(fs.readFileSync(file)));
if (stale.length) {
  process.stderr.write(`Missing or stale generated protocol files:\n${stale.map(file => path.relative(root, file)).join('\n')}\n`);
  process.exit(1);
}
