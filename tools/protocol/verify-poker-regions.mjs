import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { compatibilityErrors } from './compatibility.mjs';
import { constantName, loadProtocol } from './protocol-model.mjs';

const serverRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const projectRoot = path.resolve(serverRoot, '..');
const sourcePath = path.join(serverRoot, 'protocol/aoo-protocol-v2.json');
const baselinePath = path.join(serverRoot, 'protocol/aoo-protocol-v2.baseline.json');
const scopePath = path.join(projectRoot, 'docs/05-当前状态/PDK-新地区范围与编码表.md');
const clientPath = path.join(projectRoot, 'Client/assets/Common/Code/Runtime/network/ProtocolClient.ts');
const gatewayPath = path.join(serverRoot, 'server/Gateway/src/main/java/com/aoo/bcg/gateway/GatewayRoomBroadcastHub.java');
const javaGeneratedPath = path.join(serverRoot, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/protocol/GeneratedProtocolIds.java');
const tsGeneratedPath = path.join(projectRoot, 'Client/assets/Common/Code/Runtime/network/GeneratedProtocolIds.ts');
const problems = [];

function expect(condition, message) {
  if (!condition) problems.push(message);
}

function requiredIncludes(message, side, fields) {
  const required = message?.[side]?.required;
  return Array.isArray(required) && fields.every(field => required.includes(field));
}

function checkDispatch(messages, code) {
  const id = `poker.${code}.dispatch`;
  const message = messages.get(id);
  expect(message !== undefined, `${id}: missing`);
  if (!message) return;
  expect(message.kind === 'req', `${id}: kind must be req`);
  expect(message.write === true, `${id}: write must be true`);
  expect(message.idempotency === 'required', `${id}: idempotency must be required`);
  expect(requiredIncludes(message, 'request', ['action', 'payload']), `${id}: request requires action and payload`);
  expect(message.stage === 'M5', `${id}: stage must be M5`);
  expect(message.transport === 'WSS', `${id}: transport must be WSS`);
  expect(message.auth === 'room_session', `${id}: auth must be room_session`);
}

function checkStatePush(messages, code) {
  const id = `poker.${code}.state_push`;
  const message = messages.get(id);
  expect(message !== undefined, `${id}: missing`);
  if (!message) return;
  expect(message.kind === 'push', `${id}: kind must be push`);
  expect(message.write === false, `${id}: write must be false`);
  expect(requiredIncludes(message, 'response', ['payload', 'stateVersion', 'serverSeq']),
    `${id}: response requires payload, stateVersion and serverSeq`);
  expect(message.stage === 'M5', `${id}: stage must be M5`);
  expect(message.auth === 'room_session', `${id}: auth must be room_session`);
}

try {
  const scope = fs.readFileSync(scopePath, 'utf8');
  const table = scope.match(/## 二、编码分配[\s\S]*?\*\*禁用别名\*\*/)?.[0] ?? '';
  const regions = [...new Set(table.match(/[A-Z]{2}201/g) ?? [])];
  expect(regions.length === 25, `scope table: expected 25 new regions, found ${regions.length}`);

  const current = loadProtocol(sourcePath);
  const baseline = loadProtocol(baselinePath);
  const messages = new Map(current.messages.map(message => [message.msgId, message]));
  for (const code of regions) {
    checkDispatch(messages, code);
    checkStatePush(messages, code);
  }
  for (const code of ['CD201', 'NJ201', 'LS201']) {
    checkDispatch(messages, code);
    checkStatePush(messages, code);
  }
  for (const code of ['CD299', 'CN298', 'CN297']) {
    checkDispatch(messages, code);
    expect(!messages.has(`poker.${code}.state_push`), `poker.${code}.state_push: must not be declared`);
  }

  expect(Array.isArray(current.root.reservedMessageIds) && current.root.reservedMessageIds.length === 0,
    'reservedMessageIds must be []');
  for (const issue of compatibilityErrors(current.root, current.messages, baseline.root, baseline.messages)) {
    problems.push(`compatibility: ${issue}`);
  }
  const client = fs.readFileSync(clientPath, 'utf8');
  expect(!client.includes('CANONICAL_POKER_RUNTIME'), 'ProtocolClient.ts: CANONICAL_POKER_RUNTIME remains');
  expect(!/\(\?:[A-Z]{2}[1-5][0-9]{2}\|/.test(client), 'ProtocolClient.ts: hard-coded poker region alternation remains');
  expect(client.includes("import { ProtocolDefinitions } from './GeneratedProtocolIds';"),
    'ProtocolClient.ts: ProtocolDefinitions import missing');

  const gateway = fs.readFileSync(gatewayPath, 'utf8');
  expect(gateway.includes('GeneratedProtocolIds.DEFINITIONS'), 'GatewayRoomBroadcastHub.java: generated registry lookup missing');
  expect(!/[A-Z]{2}[1-5][0-9]{2}\|/.test(gateway), 'GatewayRoomBroadcastHub.java: hard-coded poker region alternation remains');

  const javaGenerated = fs.readFileSync(javaGeneratedPath, 'utf8');
  const tsGenerated = fs.readFileSync(tsGeneratedPath, 'utf8');
  for (const code of regions) {
    for (const suffix of ['dispatch', 'state_push']) {
      const id = `poker.${code}.${suffix}`;
      expect(javaGenerated.includes(`public static final String ${constantName(id)} = "${id}";`),
        `Java generated constant missing: ${constantName(id)}`);
      expect(tsGenerated.includes(`'${id}':`), `TypeScript generated definition missing: ${id}`);
    }
  }

  if (problems.length) {
    for (const problem of problems) process.stderr.write(`${problem}\n`);
    process.exitCode = 1;
  } else {
    process.stdout.write(`${JSON.stringify({ ok: true, regions: regions.length, messages: current.messages.length,
      registryVersion: current.root.messageRegistryVersion })}\n`);
  }
} catch (error) {
  process.stderr.write(`${error instanceof Error ? error.stack ?? error.message : String(error)}\n`);
  process.exitCode = 1;
}
