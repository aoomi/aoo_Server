import fs from 'node:fs';
import path from 'node:path';

export function assertSafeInput(file, root, maxBytes = 2 * 1024 * 1024) {
  const resolved = path.resolve(file);
  const base = path.resolve(root) + path.sep;
  if (!resolved.startsWith(base)) throw new Error(`Protocol input escapes repository: ${resolved}`);
  const stat = fs.lstatSync(resolved);
  if (!stat.isFile() || stat.isSymbolicLink()) throw new Error(`Protocol input must be a regular non-symlink file: ${resolved}`);
  if (stat.size > maxBytes) throw new Error(`Protocol input exceeds ${maxBytes} bytes: ${resolved}`);
}

export function assertAllowedOutputs(outputs, allowlist) {
  const allowed = new Set(allowlist.map(file => path.resolve(file)));
  for (const file of outputs) {
    const resolved = path.resolve(file);
    if (!allowed.has(resolved)) throw new Error(`Protocol output is not allowlisted: ${resolved}`);
    if (fs.existsSync(resolved) && fs.lstatSync(resolved).isSymbolicLink()) throw new Error(`Protocol output cannot be a symlink: ${resolved}`);
    let parent = path.dirname(resolved);
    while (!fs.existsSync(parent)) parent = path.dirname(parent);
    if (fs.lstatSync(parent).isSymbolicLink()) throw new Error(`Protocol output ancestor cannot be a symlink: ${parent}`);
  }
}
