import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { assertAllowedOutputs, assertSafeInput } from './path-security.mjs';

const root=fs.mkdtempSync(path.join(os.tmpdir(),'aoo-protocol-security-'));
const input=path.join(root,'source.json');fs.writeFileSync(input,'{}');
assert.doesNotThrow(()=>assertSafeInput(input,root));
assert.throws(()=>assertSafeInput('/etc/hosts',root),/escapes repository/);
const link=path.join(root,'link.json');fs.symlinkSync(input,link);assert.throws(()=>assertSafeInput(link,root),/non-symlink/);
const output=path.join(root,'output.txt');assert.doesNotThrow(()=>assertAllowedOutputs([output],[output]));assert.throws(()=>assertAllowedOutputs([output],[path.join(root,'other')]),/not allowlisted/);
fs.rmSync(root,{recursive:true,force:true});
