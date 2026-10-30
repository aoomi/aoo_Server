import { createRequire } from 'node:module';
import { readFile, writeFile, readdir } from 'node:fs/promises';
import { resolve, join } from 'node:path';
import { createHash } from 'node:crypto';
import { pathToFileURL } from 'node:url';

const SHEETS = ['固定局数', '循环场次', '带入额度', '说明来源'];
const BOOKS = ['跑得快', '麻将', '牛牛', '金花', '扯旋'].map(x => `${x}房卡收费.xlsx`);
const INTEGER_COLUMNS = new Set([2, 6, 7, 10]);
const empty = x => x === null || x === undefined || x === '';
const hash = b => createHash('sha256').update(b).digest('hex');

/** Structural extraction only. Financial rules belong to the Java validator. */
export function extractSheet(name, values, formulas = []) {
  if (!SHEETS.includes(name)) throw new Error('UNKNOWN_SHEET');
  const notes = name === '说明来源';
  const maxRows = notes ? 100 : 5000, maxCols = notes ? 2 : 20;
  if (values.length > maxRows) throw new Error('SHEET_ROW_LIMIT');
  if (formulas.some(row => row.some(x => !empty(x)))) throw new Error('FORMULA_NOT_ALLOWED');
  const clean = values.map(row => {
    if (row.slice(maxCols).some(x => !empty(x))) throw new Error('EXTRA_COLUMN');
    return Array.from({ length: maxCols }, (_, i) => {
      const x = row[i] ?? null;
      if (typeof x === 'string' && /^#(REF!|VALUE!|DIV\/0!|N\/A|NAME\?|NUM!|NULL!|SPILL!|CALC!)/.test(x)) throw new Error('CELL_ERROR');
      if (x !== null && !['string', 'number', 'boolean'].includes(typeof x)) throw new Error('UNSUPPORTED_CELL');
      if (!notes && INTEGER_COLUMNS.has(i) && typeof x === 'number' && !Number.isSafeInteger(x)) throw new Error('UNSAFE_INTEGER');
      return x;
    });
  });
  while (clean.length && clean.at(-1).every(empty)) clean.pop();
  if (notes) return { name, headers: [], rows: [], notes: clean };
  return { name, headers: clean.shift() ?? [], rows: clean.map((values, i) => ({ excelRow: i + 2, values, hasFormula: false })), notes: [] };
}

export async function run(argv) {
  if (argv.length !== 6) throw new Error('ARGUMENTS');
  const args = Object.fromEntries(Array.from({ length: 3 }, (_, i) => [argv[i * 2], argv[i * 2 + 1]]));
  if (!args['--workbooks'] || !args['--output'] || !args['--artifact-runtime-root']) throw new Error('ARGUMENTS');
  const require = createRequire(join(resolve(args['--artifact-runtime-root']), 'loader.cjs'));
  const { FileBlob, SpreadsheetFile } = await import(pathToFileURL(require.resolve('@oai/artifact-tool')).href);
  const directory = resolve(args['--workbooks']);
  const actual = (await readdir(directory)).filter(x => x.endsWith('.xlsx')).sort();
  if (JSON.stringify(actual) !== JSON.stringify([...BOOKS].sort())) throw new Error('WORKBOOK_SET');
  const books = [];
  for (const fileName of BOOKS) {
    const path = join(directory, fileName), before = hash(await readFile(path));
    const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(path));
    const names = workbook.worksheets.items.map(x => x.name);
    if (names.length !== 4 || SHEETS.some(x => !names.includes(x))) throw new Error('SHEET_SET');
    const sheets = SHEETS.map(name => {
      const range = workbook.worksheets.getItem(name).getUsedRange(true);
      return extractSheet(name, range.values, range.formulas);
    });
    if (before !== hash(await readFile(path))) throw new Error('INPUT_CHANGED');
    books.push({ fileName, fileHash: before, sheets });
  }
  await writeFile(resolve(args['--output']), JSON.stringify({ schemaVersion: 1, books }, null, 2), { flag: 'wx' });
}
if (process.argv[1] && resolve(process.argv[1]) === resolve(new URL(import.meta.url).pathname)) {
  run(process.argv.slice(2)).catch(error => { console.error(error.message); process.exitCode = 1; });
}
