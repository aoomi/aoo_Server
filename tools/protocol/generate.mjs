import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { constantName, loadProtocol } from './protocol-model.mjs';
import { assertAllowedOutputs, assertSafeInput } from './path-security.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const source = path.join(root, 'protocol/aoo-protocol-v2.json');
const { messages } = loadProtocol(source);
const tsTarget = path.resolve(root, '../Client/assets/Common/Code/Runtime/network/GeneratedProtocolIds.ts');
const javaTarget = path.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/protocol/GeneratedProtocolIds.java');
const docsTarget = path.join(root, 'docs/generated/protocol-reference.md');
assertSafeInput(source, root);
assertAllowedOutputs([tsTarget, javaTarget, docsTarget], [
  path.resolve(root, '../Client/assets/Common/Code/Runtime/network/GeneratedProtocolIds.ts'),
  path.join(root, 'server/GameSPI/src/main/java/com/aoo/bcg/gamespi/protocol/GeneratedProtocolIds.java'),
  path.join(root, 'docs/generated/protocol-reference.md')
]);

const quote = value => JSON.stringify(value);
const tsType = schema => {
    if (typeof schema === 'string') {
        return ({ string: 'string', integer: 'number', number: 'number', boolean: 'boolean', object: 'Readonly<Record<string, unknown>>', array: 'ReadonlyArray<unknown>' })[schema] || 'unknown';
    }
    if (!schema || typeof schema !== 'object') return 'unknown';
    if (Array.isArray(schema.enum)) return schema.enum.map(quote).join(' | ') || 'never';
    if (schema.type === 'string') return 'string';
    if (schema.type === 'integer' || schema.type === 'number') return 'number';
    if (schema.type === 'boolean') return 'boolean';
    if (schema.type === 'array') return `ReadonlyArray<${tsType(schema.items)}>`;
    if (schema.type === 'object' || schema.properties) {
        const required = new Set(schema.required || []);
        return `{ ${Object.entries(schema.properties || {}).map(([name, value]) => `${quote(name)}${required.has(name) ? '' : '?'}: ${tsType(value)}`).join('; ')} }`;
    }
    return 'unknown';
};
const bodyType = shape => tsType({ type: 'object', properties: shape?.properties || {}, required: shape?.required || [] });
const javaString = value => quote(String(value)).replace(/\\u2028|\\u2029/g, '');
const javaShape = shape => {
    const required = shape?.required || [];
    const properties = Object.entries(shape?.properties || {});
    const propertyMap = properties.length
        ? `Map.ofEntries(${properties.map(([name, schema]) => `Map.entry(${javaString(name)}, ${javaString(JSON.stringify(schema))})`).join(', ')})`
        : 'Map.of()';
    return `new Shape(Set.of(${required.map(javaString).join(', ')}), ${propertyMap})`;
};
const ts = `// Generated from Server/protocol/aoo-protocol-v2.json. Do not edit.\nexport const ProtocolIds = {\n${messages.map(m => `    ${constantName(m.msgId)}: '${m.msgId}',`).join('\n')}\n} as const;\nexport type ProtocolId = typeof ProtocolIds[keyof typeof ProtocolIds];\nexport const ProtocolDefinitions = {\n${messages.map(m => `    '${m.msgId}': ${JSON.stringify({ kind: m.kind, direction: m.direction, transport: m.transport, version: m.version, stage: m.stage, auth: m.auth, write: m.write, idempotency: m.idempotency, request: m.request, response: m.response, errors: m.errors })},`).join('\n')}\n} as const;\nexport type ProtocolDefinition = typeof ProtocolDefinitions[ProtocolId];\nexport interface ProtocolRequestBodies {\n${messages.map(m => `    '${m.msgId}': ${bodyType(m.request)};`).join('\n')}\n}\nexport interface ProtocolResponseBodies {\n${messages.map(m => `    '${m.msgId}': ${bodyType(m.response)};`).join('\n')}\n}\nexport type ProtocolRequestBody<K extends ProtocolId> = ProtocolRequestBodies[K];\nexport type ProtocolResponseBody<K extends ProtocolId> = ProtocolResponseBodies[K];\n`;
const java = `// Generated from Server/protocol/aoo-protocol-v2.json. Do not edit.\npackage com.aoo.bcg.gamespi.protocol;\n\nimport java.util.Map;\nimport java.util.Set;\n\npublic final class GeneratedProtocolIds {\n    public record Shape(Set<String> required, Map<String, String> properties) {}\n    public record Definition(String kind, String direction, String transport, String version, String stage, String auth, boolean write, String idempotency, Shape request, Shape response, Set<Integer> errors) {}\n    private GeneratedProtocolIds() {}\n${messages.map(m => `    public static final String ${constantName(m.msgId)} = "${m.msgId}";`).join('\n')}\n    public static final Map<String, Definition> DEFINITIONS = Map.ofEntries(\n${messages.map(m => `        Map.entry(${constantName(m.msgId)}, new Definition(${quote(m.kind)}, ${quote(m.direction)}, ${quote(m.transport)}, ${quote(m.version)}, ${quote(m.stage)}, ${quote(m.auth)}, ${m.write}, ${quote(m.idempotency)}, ${javaShape(m.request)}, ${javaShape(m.response)}, Set.of(${m.errors.join(', ')})))`).join(',\n')}\n    );\n}\n`;
const sampleValue = type => ({ string: 'string', integer: 0, number: 0, boolean: false, object: {}, array: [] })[type] ?? null;
const sampleBody = shape => Object.fromEntries(Object.entries(shape?.properties || {}).map(([name, type]) => [name, sampleValue(type)]));
const shapeRows = shape => Object.entries(shape?.properties || {}).map(([name, type]) => `| ${name} | ${type} | ${(shape.required || []).includes(name) ? '是' : '否'} |`).join('\n') || '| - | - | - |';
const docs = `# Aoo Protocol V2 接口参考\n\n> 由 \`protocol/aoo-protocol-v2.json\` 自动生成，禁止手工编辑。协议版本：${messages[0]?.version || '2.0'}。\n\n${messages.map(m => `## \`${m.msgId}\`\n\n| 版本 | 方向 | 传输 | 类型 | 阶段 | 鉴权 | 写操作 | 幂等 | 错误码 |\n|---|---|---|---|---|---|---|---|---|\n| ${m.version} | ${m.direction} | ${m.transport} | ${m.kind} | ${m.stage} | ${m.auth} | ${m.write ? '是' : '否'} | ${m.idempotency} | ${m.errors.join(', ')} |\n\n### 请求字段\n\n| 字段 | 类型 | 必填 |\n|---|---|---|\n${shapeRows(m.request)}\n\n### 响应字段\n\n| 字段 | 类型 | 必填 |\n|---|---|---|\n${shapeRows(m.response)}\n\n### 示例\n\n\`\`\`json\n${JSON.stringify({ protocolVersion: m.version, msgId: m.msgId, kind: m.kind, requestId: 'request-id', seq: 1, timestamp: 0, traceId: 'trace-id', body: sampleBody(m.request) }, null, 2)}\n\`\`\`\n`).join('\n')}\n`;

fs.mkdirSync(path.dirname(tsTarget), { recursive: true });
fs.mkdirSync(path.dirname(javaTarget), { recursive: true });
fs.mkdirSync(path.dirname(docsTarget), { recursive: true });
fs.writeFileSync(tsTarget, ts);
fs.writeFileSync(javaTarget, java);
fs.writeFileSync(docsTarget, docs);
