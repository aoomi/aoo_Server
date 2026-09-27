import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const serverRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const projectRoot = path.resolve(serverRoot, '..');
const protocolClientUrl = pathToFileURL(path.join(projectRoot,
  'Client/assets/Common/Code/Runtime/network/ProtocolClient.ts')).href;
const scopePath = path.join(projectRoot, 'docs/05-当前状态/PDK-新地区范围与编码表.md');
const scope = fs.readFileSync(scopePath, 'utf8');
const table = scope.match(/## 二、编码分配[\s\S]*?\*\*禁用别名\*\*/)?.[0] ?? '';
const regions = [...new Set(table.match(/[A-Z]{2}201/g) ?? [])];
const established = ['CD201', 'NJ201', 'LS201', 'CD299', 'CN298', 'CN297'];
const routeIds = [...new Set([...regions, ...established].flatMap(code =>
  [`poker.${code}.dispatch`, `poker.${code}.state_push`]))];
const dispatchIds = regions.map(code => `poker.${code}.dispatch`);
const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), 'aoo-pdk-routing-'));
const problems = [];

function expect(condition, message) {
  if (!condition) problems.push(message);
}

try {
  const hooksPath = path.join(tempDir, 'hooks.mjs');
  const registerPath = path.join(tempDir, 'register.mjs');
  const harnessPath = path.join(tempDir, 'harness.mjs');
  fs.writeFileSync(hooksPath, `import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
export async function resolve(specifier, context, next) {
  if (specifier.startsWith('.') && !path.extname(specifier)) {
    const base = path.dirname(fileURLToPath(context.parentURL));
    for (const ext of ['.ts', '/index.ts']) {
      const candidate = path.join(base, specifier + ext);
      if (existsSync(candidate)) return next(pathToFileURL(candidate).href, context);
    }
  }
  return next(specifier, context);
}
`);
  fs.writeFileSync(registerPath, `import { register } from 'node:module'; register('./hooks.mjs', import.meta.url);\n`);
  fs.writeFileSync(harnessPath, `const mod = await import(${JSON.stringify(protocolClientUrl)});
const baseProto = Object.getPrototypeOf(mod.ProtocolClient.prototype);
let captured = null;
baseProto.request = async function (event, outbound) {
  this.prepareOutboundBody(event, outbound);
  captured = JSON.parse(JSON.stringify(outbound.envelope));
  return { protocolVersion: '2.0', kind: 'resp', requestId: captured.requestId, traceId: captured.traceId,
    msgId: captured.msgId, seq: captured.seq, timestamp: captured.timestamp, body: {} };
};
const client = new mod.ProtocolClient();
const routes = {};
for (const id of ${JSON.stringify([...routeIds, 'poker.ZZ999.dispatch'])}) routes[id] = client.getRoute(id);
const envelopes = {};
for (const id of ${JSON.stringify([...dispatchIds, 'poker.ZZ999.dispatch'])}) {
  captured = null;
  await client.request(id, { roomId: '9001', roundNo: 0, playVersion: 'pdk-v1', action: 'play', payload: {} }, 1000);
  envelopes[id] = captured;
}
let legacyPdkError = '';
try {
  await client.request('poker.pdk.dispatch',
    { roomId: '9001', roundNo: 0, playVersion: 'pdk-v1', action: 'play', payload: {} }, 1000);
} catch (error) {
  legacyPdkError = error instanceof Error ? error.message : String(error);
}
console.log(JSON.stringify({ routes, envelopes, legacyPdkError }));
`);

  const output = execFileSync(process.execPath,
    ['--experimental-transform-types', '--import', registerPath, harnessPath], { encoding: 'utf8' });
  const result = JSON.parse(output.trim().split(/\r?\n/).at(-1));

  for (const code of regions) {
    for (const suffix of ['dispatch', 'state_push']) {
      const id = `poker.${code}.${suffix}`;
      expect(result.routes[id]?.canonicalMsgId === id, `${id}: canonicalMsgId mismatch`);
      expect(result.routes[id]?.stage === 'M5', `${id}: stage must be M5`);
    }
    const id = `poker.${code}.dispatch`;
    const envelope = result.envelopes[id];
    expect(envelope?.msgId === id, `${id}: outbound msgId mismatch`);
    expect(envelope?.roomId === '9001' && envelope?.roundNo === 0 && envelope?.playVersion === 'pdk-v1',
      `${id}: outbound room authority missing`);
  }
  for (const code of established) {
    for (const suffix of ['dispatch', 'state_push']) {
      const id = `poker.${code}.${suffix}`;
      expect(result.routes[id]?.canonicalMsgId === id, `${id}: established canonicalMsgId regressed`);
      expect(result.routes[id]?.stage === 'M5', `${id}: established stage regressed`);
    }
  }
  expect(result.routes['poker.ZZ999.dispatch']?.canonicalMsgId === 'hall.dispatch',
    'poker.ZZ999.dispatch: must route to hall.dispatch');
  expect(result.envelopes['poker.ZZ999.dispatch']?.msgId === 'hall.dispatch',
    'poker.ZZ999.dispatch: outbound must use hall.dispatch');
  expect(String(result.legacyPdkError).includes('旧 pdk 写协议已停用'),
    'poker.pdk.dispatch: expected legacy write rejection');

  if (problems.length) {
    for (const problem of problems) process.stderr.write(`${problem}\n`);
    process.exitCode = 1;
  } else {
    process.stdout.write(`${JSON.stringify({ ok: true, regions: regions.length, routes: routeIds.length + 1,
      envelopes: dispatchIds.length + 1 })}\n`);
  }
} catch (error) {
  process.stderr.write(`${error instanceof Error ? error.stack ?? error.message : String(error)}\n`);
  process.exitCode = 1;
} finally {
  fs.rmSync(tempDir, { recursive: true, force: true });
}
