import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const pinned = fs.readFileSync(path.join(root, '.node-version'), 'utf8').trim();
if (process.versions.node !== pinned) throw new Error(`Node ${pinned} required, current ${process.versions.node}`);
const outputs = [
  path.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/protocol/GeneratedProtocolIds.java'),
  path.resolve(root, '../Client/assets/Common/Code/Runtime/network/GeneratedProtocolIds.ts'),
  path.join(root, 'docs/generated/protocol-reference.md')
];
const digest = () => outputs.map(file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex'));
const generate = () => execFileSync(process.execPath, [path.join(root, 'tools/protocol/generate.mjs')], { stdio: 'inherit' });
generate();
const first = digest();
generate();
const second = digest();
if (JSON.stringify(first) !== JSON.stringify(second)) throw new Error('Protocol generation is not deterministic');
process.stdout.write(`${JSON.stringify({ node: pinned, outputs: outputs.map((file, index) => ({ path: path.relative(root, file), sha256: second[index] })) }, null, 2)}\n`);
