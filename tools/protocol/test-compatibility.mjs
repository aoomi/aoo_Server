import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadProtocol } from './protocol-model.mjs';
import { compatibilityErrors } from './compatibility.mjs';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const baseline = loadProtocol(path.join(root, 'protocol/aoo-protocol-v2.baseline.json'));
const clone = value => JSON.parse(JSON.stringify(value));
const evaluate = mutate => { const current = clone(baseline.root); current.messageRegistryVersion = 1; current.reservedMessageIds = []; mutate(current); return compatibilityErrors(current, current.messages, baseline.root, baseline.messages); };
assert.ok(evaluate(current => { delete current.messages[0].request.properties[Object.keys(current.messages[0].request.properties)[0]]; }).some(error => error.includes('.removed')));
assert.ok(evaluate(current => { const field=Object.keys(current.messages[0].request.properties)[0]; current.messages[0].request.properties[field]='boolean'; }).some(error => error.includes('.type')));
assert.ok(evaluate(current => { const field=Object.keys(current.messages[0].request.properties).find(name=>!current.messages[0].request.required.includes(name)); if(field)current.messages[0].request.required.push(field); else {current.messages[0].request.properties.newField='string';current.messages[0].request.required.push('newField');} }).some(error => error.includes('.newRequired')));
assert.ok(evaluate(current => { current.reservedMessageIds.push(current.messages[0].msgId); current.messages.shift(); }).some(error => error.includes('major version')));
assert.equal(evaluate(current => { current.messages[0].request.properties.optionalAdded='string'; current.messages[0].version='2.1'; }).filter(error => error.includes('breaking')).length,0);
