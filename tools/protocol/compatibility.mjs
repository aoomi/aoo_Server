export function compatibilityErrors(currentRoot, currentMessages, baselineRoot, baselineMessages) {
  const errors = [];
  const current = new Map(currentMessages.map(message => [message.msgId, message]));
  const reserved = new Set(currentRoot.reservedMessageIds ?? []);
  const currentMajor = major(currentRoot.protocolVersion);
  const baselineMajor = major(baselineRoot.protocolVersion);
  for (const oldMessage of baselineMessages) {
    const next = current.get(oldMessage.msgId);
    if (!next) {
      if (!reserved.has(oldMessage.msgId)) errors.push(`${oldMessage.msgId}: removed message must remain reserved`);
      if (currentMajor === baselineMajor) errors.push(`${oldMessage.msgId}: message removal requires a protocol major version`);
      continue;
    }
    const changes = [];
    for (const field of ['kind', 'direction', 'transport', 'auth', 'write', 'idempotency']) {
      if (canonicalJson(oldMessage[field]) !== canonicalJson(next[field])) changes.push(field);
    }
    for (const side of ['request', 'response']) compareShape(oldMessage[side], next[side], side, changes);
    for (const code of oldMessage.errors ?? []) if (!(next.errors ?? []).includes(code)) changes.push(`errors.${code}.removed`);
    if (changes.length && currentMajor === baselineMajor) errors.push(`${oldMessage.msgId}: breaking changes require a protocol major version (${changes.join(', ')})`);
    if (canonicalJson(oldMessage) !== canonicalJson(next) && oldMessage.version === next.version) errors.push(`${oldMessage.msgId}: changed without a message version change`);
  }
  return errors;
}

function compareShape(oldShape, nextShape, side, changes) {
  const oldProperties = oldShape?.properties ?? {};
  const nextProperties = nextShape?.properties ?? {};
  const oldRequired = new Set(oldShape?.required ?? []);
  const nextRequired = new Set(nextShape?.required ?? []);
  for (const [field, type] of Object.entries(oldProperties)) {
    if (!(field in nextProperties)) changes.push(`${side}.${field}.removed`);
    else if (canonicalJson(type) !== canonicalJson(nextProperties[field])) changes.push(`${side}.${field}.type`);
  }
  for (const field of nextRequired) if (!oldRequired.has(field)) changes.push(`${side}.${field}.newRequired`);
}

function major(version) {
  const match = /^(\d+)\./.exec(String(version));
  if (!match) throw new Error(`Invalid protocol version: ${version}`);
  return Number(match[1]);
}

function canonicalJson(value) {
  if (Array.isArray(value)) {
    const entries = value.map(canonicalJson);
    if (value.every(item => item === null || ['string', 'number', 'boolean'].includes(typeof item))) entries.sort();
    return `[${entries.join(',')}]`;
  }
  if (value && typeof value === 'object') return `{${Object.keys(value).sort().map(key => `${JSON.stringify(key)}:${canonicalJson(value[key])}`).join(',')}}`;
  return JSON.stringify(value);
}
