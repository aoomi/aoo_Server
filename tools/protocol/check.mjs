import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadProtocol } from './protocol-model.mjs';
import { compatibilityErrors } from './compatibility.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const source = path.join(root, 'protocol/aoo-protocol-v2.json');
const baselineFile = path.join(root, 'protocol/aoo-protocol-v2.baseline.json');
const { root: protocol, messages } = loadProtocol(source);

const errors = [];
const reservedMessageIds = new Set(protocol.reservedMessageIds ?? []);
if (!Number.isInteger(protocol.messageRegistryVersion) || protocol.messageRegistryVersion < 1) {
  errors.push('messageRegistryVersion must be a positive integer');
}
for (const id of reservedMessageIds) {
  if (!/^[a-z][a-z0-9]*(\.(?:[a-z0-9_]+|[A-Z]{2}[1-5][0-9]{2}))+$/.test(id)) errors.push(`${id}: invalid reserved msgId`);
  if (messages.some(message => message.msgId === id)) errors.push(`${id}: reserved msgId cannot be active`);
}

const clientAssets = path.resolve(root, '../Client/assets');
for (const file of walk(clientAssets)) {
  if (!/\.(ts|js)$/.test(file)) continue;
  const relative = path.relative(clientAssets, file).replaceAll('\\', '/');
  const text = fs.readFileSync(file, 'utf8');
  if (/(^|[^\w.])fetch\s*\(/m.test(text) && relative !== 'Common/Code/Runtime/network/ProtocolHttpClient.ts') {
    errors.push(`${relative}: direct fetch is forbidden; use ProtocolHttpClient`);
  }
  if (/new\s+WebSocket\s*\(/.test(text)
      && !relative.endsWith('/network/LegacyWebSocketClient.ts')) {
    errors.push(`${relative}: direct WebSocket is forbidden; use ProtocolClient`);
  }
  for (const match of text.matchAll(/http:\/\/([^/'"`]+)/g)) {
    if (!['127.0.0.1', 'localhost', '[::1]'].includes(match[1].split(':')[0])) {
      errors.push(`${relative}: production URL must use HTTPS (${match[0]})`);
    }
  }
}
const declaredRanges = Object.values(protocol.errorCodeRanges ?? {}).map(value => {
  const match = /^(\d+)-(\d+)$/.exec(value);
  return match ? [Number(match[1]), Number(match[2])] : null;
}).filter(Boolean);
for (const message of messages) {
  if (!message.version) errors.push(`${message.msgId}: version is required`);
  for (const code of message.errors) {
    if (!declaredRanges.some(([start, end]) => code >= start && code <= end)) errors.push(`${message.msgId}: error ${code} is outside declared ranges`);
  }
}

if (fs.existsSync(baselineFile)) {
  const baseline = loadProtocol(baselineFile);
  errors.push(...compatibilityErrors(protocol, messages, baseline.root, baseline.messages));
}

if (errors.length) {
  process.stderr.write(`${errors.join('\n')}\n`);
  process.exit(1);
}

function walk(directory) {
  if (!fs.existsSync(directory)) return [];
  const files = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const target = path.join(directory, entry.name);
    if (entry.isDirectory()) files.push(...walk(target));
    else files.push(target);
  }
  return files;
}
