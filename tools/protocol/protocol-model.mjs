import fs from 'node:fs';

export function loadProtocol(file) {
  const root = JSON.parse(fs.readFileSync(file, 'utf8'));
  const source = root.messages ?? root.protocols ?? root.routes;
  const messages = Array.isArray(source)
    ? source
    : Object.entries(source ?? {}).map(([msgId, value]) => ({ msgId, ...value }));
  if (messages.length === 0) throw new Error('Protocol source contains no messages');
  const ids = new Set();
  const stages = new Set(['M1', 'M2', 'M3', 'M4', 'M5', 'M6', 'M7', 'M8']);
  const kinds = new Set(['req', 'push', 'system']);
  const directions = new Set(['client_to_server', 'server_to_client', 'bidirectional']);
  const transports = new Set(['HTTPS', 'WSS']);
  const idempotencyModes = new Set(['required', 'optional', 'none']);
  for (const message of messages) {
    if (typeof message.msgId !== 'string' || !/^[a-z][a-z0-9]*(\.[a-z0-9_]+)+$/.test(message.msgId)) {
      throw new Error(`Invalid msgId: ${message.msgId}`);
    }
    if (ids.has(message.msgId)) throw new Error(`Duplicate msgId: ${message.msgId}`);
    ids.add(message.msgId);
    if (!kinds.has(message.kind)) throw new Error(`${message.msgId}: invalid kind`);
    if (!directions.has(message.direction)) throw new Error(`${message.msgId}: invalid direction`);
    if (!transports.has(message.transport)) throw new Error(`${message.msgId}: invalid transport`);
    if (!stages.has(message.stage)) throw new Error(`${message.msgId}: invalid stage`);
    if (typeof message.auth !== 'string' || !message.auth) throw new Error(`${message.msgId}: auth is required`);
    if (typeof message.write !== 'boolean') throw new Error(`${message.msgId}: write flag is required`);
    if (!idempotencyModes.has(message.idempotency)) throw new Error(`${message.msgId}: invalid idempotency mode`);
    if (message.write && message.idempotency !== 'required') throw new Error(`${message.msgId}: write messages require idempotency`);
    validateShape(message.msgId, 'request', message.request);
    validateShape(message.msgId, 'response', message.response);
    if (message.kind === 'req' && message.transport === 'WSS') {
      for (const field of ['action', 'payload']) {
        if (!message.request.required.includes(field)) throw new Error(`${message.msgId}: WSS request requires ${field}`);
      }
    }
    if (message.kind === 'push' && message.msgId.endsWith('state_push')
        && message.msgId !== 'common.room.state_push') {
      for (const field of ['action', 'payload']) {
        if (!message.response.required.includes(field)) throw new Error(`${message.msgId}: compatibility push requires ${field}`);
      }
    }
    if (!Array.isArray(message.errors) || message.errors.some(code => !Number.isInteger(code) || code <= 0)) {
      throw new Error(`${message.msgId}: errors must be positive integer codes`);
    }
  }
  return { root, messages: messages.sort((a, b) => a.msgId.localeCompare(b.msgId)) };
}

function validateShape(msgId, side, shape) {
  if (!shape || shape.type !== 'object' || !Array.isArray(shape.required) || !shape.properties || typeof shape.properties !== 'object') {
    throw new Error(`${msgId}: ${side} must define object type, required and properties`);
  }
  for (const field of shape.required) {
    if (!(field in shape.properties)) throw new Error(`${msgId}: ${side}.${field} is required but undefined`);
  }
  for (const [field, type] of Object.entries(shape.properties)) {
    if (!/^[a-z][a-zA-Z0-9]*$/.test(field)) throw new Error(`${msgId}: invalid field name ${side}.${field}`);
    if (!['string', 'integer', 'number', 'boolean', 'object', 'array'].includes(type)) {
      throw new Error(`${msgId}: unsupported type ${side}.${field}=${type}`);
    }
  }
}

export function constantName(msgId) {
  return msgId.replace(/[^a-zA-Z0-9]+/g, '_').replace(/^_|_$/g, '').toUpperCase();
}
