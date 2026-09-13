#!/usr/bin/env node
/**
 * Finds imports whose case does not match the file on disk.
 *
 * Windows and macOS resolve paths case-insensitively, so `./app/addThreadPage`
 * happily loads `AddThreadPage.tsx` and nothing ever complains. Metro's bundler
 * does not, and neither does Linux - so the mismatch surfaces as a release build
 * failing to bundle, or CI failing on a file that works on the machine it was
 * written on.
 *
 * That is exactly how it surfaced here: the app ran in development for months
 * and `gradlew bundleRelease` could not resolve the module.
 *
 *   node scripts/check-import-case.js
 *
 * Exits non-zero when something is wrong, so it can gate a release.
 */

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SKIP = new Set(['node_modules', '.git', 'android', 'ios', 'build', 'scripts']);
const SOURCE = /\.(t|j)sx?$/;
const EXTENSIONS = ['', '.ts', '.tsx', '.js', '.jsx', '.json'];

/** Every file, indexed by its lowercased path so a case-insensitive hit is findable. */
function index(dir, found = new Map()) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (SKIP.has(entry.name)) continue;

    const full = path.join(dir, entry.name);
    const rel = path.relative(ROOT, full).split(path.sep).join('/');

    if (entry.isDirectory()) {
      index(full, found);
    } else {
      found.set(rel.toLowerCase(), rel);
    }
  }
  return found;
}

function sources(dir, found = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (SKIP.has(entry.name)) continue;

    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      sources(full, found);
    } else if (SOURCE.test(entry.name)) {
      found.push(full);
    }
  }
  return found;
}

const files = index(ROOT);
const problems = [];

for (const file of sources(ROOT)) {
  const text = fs.readFileSync(file, 'utf8');
  const from = path.dirname(file);

  // import ... from './x'  and  require('./x')
  for (const match of text.matchAll(/(?:from\s*|require\(\s*)['"](\.[^'"]+)['"]/g)) {
    const spec = match[1];
    const base = path.relative(ROOT, path.resolve(from, spec)).split(path.sep).join('/');

    for (const ext of EXTENSIONS) {
      const candidate = (base + ext).toLowerCase();
      if (files.has(candidate)) {
        if (files.get(candidate) !== base + ext) {
          problems.push({
            file: path.relative(ROOT, file).split(path.sep).join('/'),
            spec,
            actual: files.get(candidate),
          });
        }
        break;
      }
    }
  }
}

if (problems.length === 0) {
  console.log('Import case: every relative import matches its file on disk.');
  process.exit(0);
}

console.error('Import case: these resolve on Windows and macOS but not in a release bundle.\n');
for (const { file, spec, actual } of problems) {
  console.error(`  ${file}`);
  console.error(`    imports '${spec}'`);
  console.error(`    real file is '${actual}'\n`);
}
process.exit(1);
