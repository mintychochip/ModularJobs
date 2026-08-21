/**
 * Portable docs gate for Fumadocs repos. Node builtins only, no dependencies.
 *
 * Copy to scripts/verify-docs.mjs and wire it to `npm test`. Installed there,
 * it defaults to the repo root (its own parent directory).
 *
 * Auto-detects two repo shapes:
 *   hub          — full Fumadocs Next app (package.json + app/ directory)
 *   content-only — content/docs/ owned by the repo, rendered by a hub
 *
 * Checks, in order: stack and wiring (hub), content and frontmatter,
 * navigation, links, styling, and optionally the floor-to-ceiling ladder.
 *
 * Usage:
 *   node scripts/verify-docs.mjs                    # this repo
 *   node verify-docs.mjs --root=../site-docs        # another repo, run in place
 *   DOCS_ROOT=../site-docs node verify-docs.mjs     # same, via environment
 *
 * Flags:
 *   --root=PATH         repo to check; overrides DOCS_ROOT and the default.
 *                       Required when running this file from outside the repo
 *                       it should check.
 *   --ladder[=dir,dir]  require basics/Everyday/Advanced labels on guide pages,
 *                       scoped to the directories that owe a ladder.
 *                       e.g. --ladder=player-guides
 */
import { readFileSync, readdirSync, existsSync, statSync } from 'node:fs';
import { join, dirname, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import assert from 'node:assert/strict';

// Explicit --root/DOCS_ROOT wins, so this file can check a repo it does not
// live in. Installed at scripts/verify-docs.mjs, the default is the repo root.
const rootArg = process.argv.find((a) => a.startsWith('--root='))?.slice('--root='.length);
const root = resolve(rootArg ?? process.env.DOCS_ROOT ?? join(dirname(fileURLToPath(import.meta.url)), '..'));
const contentDir = join(root, 'content/docs');

const ladderArg = process.argv.find((a) => a === '--ladder' || a.startsWith('--ladder='));
const checkLadder = Boolean(ladderArg);
const ladderScopes = (ladderArg?.split('=')[1] ?? '')
  .split(',')
  .map((s) => s.trim().replace(/\/$/, ''))
  .filter(Boolean);

const read = (rel) => readFileSync(join(root, rel), 'utf8');
const isDir = (p) => existsSync(p) && statSync(p).isDirectory();

const hasPkg = existsSync(join(root, 'package.json'));
const isHub = hasPkg && isDir(join(root, 'app'));

assert.ok(
  isDir(contentDir),
  `content/docs not found under ${root} — pass --root=PATH to point at the repo to check`,
);

// ---------------------------------------------------------------------------
// 1. Fumadocs stack and source wiring (hub only)
// ---------------------------------------------------------------------------
if (isHub) {
  const pkg = JSON.parse(read('package.json'));
  const deps = { ...pkg.dependencies, ...pkg.devDependencies };

  for (const name of ['fumadocs-core', 'fumadocs-mdx', 'next']) {
    assert.ok(deps[name], `missing dependency: ${name}`);
  }

  // fumadocs-ui is normally an npm alias to @fumadocs/base-ui.
  const uiSpec = deps['fumadocs-ui'] ?? deps['@fumadocs/base-ui'] ?? '';
  assert.ok(uiSpec, 'missing fumadocs-ui / @fumadocs/base-ui');

  // The alias must track fumadocs-core, since they ship as a matched pair.
  const uiVersion = String(uiSpec).match(/(\d+\.\d+\.\d+)/)?.[1];
  const coreVersion = String(deps['fumadocs-core']).match(/(\d+\.\d+\.\d+)/)?.[1];
  if (uiVersion && coreVersion) {
    assert.equal(
      uiVersion,
      coreVersion,
      `fumadocs-ui (${uiVersion}) must match fumadocs-core (${coreVersion})`,
    );
  }

  // Content must load through Fumadocs. Two valid layouts: a standalone
  // source.config.ts, or defineDocs called from lib/source.ts via
  // fumadocs-mdx/macro.
  assert.ok(existsSync(join(root, 'lib/source.ts')), 'lib/source.ts missing');
  const sourceTs = read('lib/source.ts');
  assert.match(sourceTs, /fumadocs-core\/source/, 'lib/source.ts must use fumadocs-core/source');
  assert.match(sourceTs, /toFumadocsSource|loader/, 'lib/source.ts must build a Fumadocs loader');

  if (existsSync(join(root, 'source.config.ts'))) {
    const sourceConfig = read('source.config.ts');
    assert.match(sourceConfig, /defineDocs/, 'source.config.ts must call defineDocs');
  } else {
    assert.match(
      sourceTs,
      /defineDocs/,
      'no source.config.ts, so lib/source.ts must call defineDocs (fumadocs-mdx/macro)',
    );
  }

  const docsLayout = existsSync(join(root, 'app/docs/layout.tsx'))
    ? join(root, 'app/docs/layout.tsx')
    : join(root, 'app/[[...slug]]/layout.tsx');
  assert.ok(existsSync(docsLayout), 'docs layout missing (app/docs/layout.tsx or app/[[...slug]]/layout.tsx)');
  const catchAll = existsSync(join(root, 'app/docs/[[...slug]]/page.tsx'))
    ? join(root, 'app/docs/[[...slug]]/page.tsx')
    : join(root, 'app/[[...slug]]/page.tsx');
  assert.ok(
    existsSync(catchAll),
    'catch-all route missing (app/docs/[[...slug]]/page.tsx or app/[[...slug]]/page.tsx)',
  );
  const pageTsx = readFileSync(catchAll, 'utf8');
  assert.match(pageTsx, /source\.getPage/, 'catch-all route must resolve pages via source.getPage');
  assert.match(pageTsx, /DocsPage|DocsTitle|DocsBody/, 'catch-all route must render Fumadocs docs components');
}

// ---------------------------------------------------------------------------
// 2. Content: walk content/docs, validate frontmatter on every page
// ---------------------------------------------------------------------------
const pages = []; // { rel, dir, slug, title, description, body }

function walk(dir) {
  for (const entry of readdirSync(dir)) {
    const abs = join(dir, entry);
    if (isDir(abs)) {
      walk(abs);
    } else if (entry.endsWith('.mdx')) {
      const raw = readFileSync(abs, 'utf8');
      const rel = relative(contentDir, abs);
      const fm = raw.match(/^---\r?\n([\s\S]*?)\r?\n---/);
      assert.ok(fm, `${rel}: missing YAML frontmatter`);

      const block = fm[1];
      const titleLine = block.match(/^title:\s*(.+)$/m);
      const descLine = block.match(/^description:\s*(.+)$/m);
      assert.ok(titleLine, `${rel}: frontmatter missing title`);
      assert.ok(descLine, `${rel}: frontmatter missing description`);

      // An unquoted colon in a value breaks the MDX YAML parse at build time
      // with "Nested mappings are not allowed".
      for (const [key, line] of [
        ['title', titleLine[1]],
        ['description', descLine[1]],
      ]) {
        const value = line.trim();
        const quoted =
          (value.startsWith('"') && value.endsWith('"')) ||
          (value.startsWith("'") && value.endsWith("'")) ||
          value.startsWith('>') ||
          value.startsWith('|');
        if (!quoted) {
          assert.ok(
            !/:\s/.test(value),
            `${rel}: unquoted colon in ${key} — wrap the value in double quotes`,
          );
        }
      }

      pages.push({
        rel,
        dir: relative(contentDir, dir) || '.',
        slug: entry.replace(/\.mdx$/, ''),
        title: titleLine[1].trim(),
        description: descLine[1].trim(),
        body: raw.slice(fm[0].length),
      });
    }
  }
}
walk(contentDir);

assert.ok(
  pages.some((p) => p.dir === '.' && p.slug === 'index'),
  'content/docs/index.mdx required',
);
assert.ok(
  pages.length >= 2,
  `need index + at least one content page, found ${pages.length}`,
);

// ---------------------------------------------------------------------------
// 3. Navigation: meta.json integrity, in both directions
// ---------------------------------------------------------------------------
assert.ok(
  existsSync(join(contentDir, 'meta.json')),
  'content/docs/meta.json sidebar config missing',
);

const reachable = new Set();

function checkMeta(dirRel) {
  const dirAbs = join(contentDir, dirRel === '.' ? '' : dirRel);
  const metaPath = join(dirAbs, 'meta.json');
  if (!existsSync(metaPath)) return;

  let meta;
  try {
    meta = JSON.parse(readFileSync(metaPath, 'utf8'));
  } catch (err) {
    assert.fail(`${join(dirRel, 'meta.json')}: invalid JSON — ${err.message}`);
  }
  assert.ok(Array.isArray(meta.pages), `${join(dirRel, 'meta.json')}: pages must be an array`);

  for (const raw of meta.pages) {
    if (typeof raw !== 'string') continue;
    // Separators ("---Section---") and Fumadocs operators ("...", "!x",
    // "[label](url)") are navigation directives, not page slugs.
    if (
      raw.startsWith('---') ||
      raw.startsWith('...') ||
      raw.startsWith('!') ||
      raw.includes('](')
    ) {
      continue;
    }
    const name = raw.replace(/^z/, '');
    const asFile = join(dirAbs, `${name}.mdx`);
    const asDir = join(dirAbs, name);

    if (existsSync(asFile)) {
      reachable.add(relative(contentDir, asFile));
    } else if (isDir(asDir)) {
      assert.ok(
        existsSync(join(asDir, 'meta.json')),
        `${join(dirRel, name)}: section folder needs its own meta.json`,
      );
      checkMeta(relative(contentDir, asDir));
    } else {
      assert.fail(
        `${join(dirRel, 'meta.json')}: "${raw}" has no matching .mdx file or folder`,
      );
    }
  }
}
checkMeta('.');

// A page listed nowhere renders at its URL but never appears in the sidebar.
for (const page of pages) {
  assert.ok(
    reachable.has(page.rel),
    `${page.rel}: not reachable from any meta.json — it renders at its URL but never appears in the sidebar`,
  );
}

// ---------------------------------------------------------------------------
// 4. Links: every internal docs link must resolve to a real page
// ---------------------------------------------------------------------------
const pageRels = new Set(pages.map((p) => p.rel));

/** Resolve a /docs/... path to a content file, or null if nothing matches. */
function resolveDocsPath(docsPath) {
  const clean = docsPath.replace(/^\/docs\/?/, '').replace(/\/$/, '');
  const candidates = clean === ''
    ? ['index.mdx']
    : [`${clean}.mdx`, join(clean, 'index.mdx')];
  return candidates.find((c) => pageRels.has(c)) ?? null;
}

const brokenLinks = [];
for (const page of pages) {
  const targets = new Set();
  // Markdown links: [label](target)
  for (const m of page.body.matchAll(/\]\(([^)\s]+)/g)) targets.add(m[1]);
  // JSX/HTML attributes: href="target"
  for (const m of page.body.matchAll(/href=["']([^"']+)["']/g)) targets.add(m[1]);

  for (const target of targets) {
    // External links, anchors, and non-http protocols are out of scope: this
    // gate never makes network calls.
    if (/^(https?:|mailto:|tel:|#|\/\/)/.test(target)) continue;
    const [path] = target.split('#');
    if (!path) continue;
    // Only /docs/* paths are resolvable against content/docs.
    if (!path.startsWith('/docs')) continue;
    if (!resolveDocsPath(path)) {
      brokenLinks.push(`${page.rel} → ${target}`);
    }
  }
}
assert.equal(
  brokenLinks.length,
  0,
  `broken internal docs links (no matching page):\n  ${brokenLinks.join('\n  ')}`,
);

// ---------------------------------------------------------------------------
// 5. Styling: the stylesheet must load the Fumadocs layers, and content
//    must not hardcode colors. Hub-only — content-only repos have no CSS.
// ---------------------------------------------------------------------------
if (isHub) {
  const globalCss = ['app/global.css', 'app/globals.css', 'styles/global.css'].find((p) =>
    existsSync(join(root, p)),
  );
  assert.ok(globalCss, 'no global stylesheet found (expected app/global.css)');
  const css = read(globalCss);

  assert.match(css, /@import\s+['"]tailwindcss['"]/, `${globalCss} must import tailwindcss`);
  assert.match(
    css,
    /@import\s+['"]fumadocs-ui\/css\/preset\.css['"]/,
    `${globalCss} must import fumadocs-ui/css/preset.css`,
  );
  assert.match(
    css,
    /@import\s+['"]fumadocs-ui\/css\/[a-z-]+\.css['"]/,
    `${globalCss} must import a fumadocs-ui color scheme (e.g. neutral.css)`,
  );

  // Any local layer the stylesheet imports (tokens.css, theme.css, ...) must
  // exist, or the build fails on a missing module.
  for (const m of css.matchAll(/@import\s+['"](\.\/[^'"]+)['"]/g)) {
    const target = join(root, dirname(globalCss), m[1]);
    assert.ok(existsSync(target), `${globalCss} imports ${m[1]}, which does not exist`);
  }

  // Theming lives in CSS; a hex in a page cannot respond to dark mode.
  const hardcoded = pages.filter((p) =>
    /#[0-9a-fA-F]{3,8}\b/.test(p.body.replace(/`[^`]*`/g, '')),
  );
  assert.equal(
    hardcoded.length,
    0,
    `hardcoded hex colors in content (use a CSS token instead): ${hardcoded
      .map((p) => p.rel)
      .join(', ')}`,
  );
}

// ---------------------------------------------------------------------------
// 6. Optional: floor-to-ceiling ladder labels (--ladder)
// ---------------------------------------------------------------------------
if (checkLadder) {
  // Scope to guide directories. Landing pages (index.mdx) are aggregations and
  // exempt; reference or contributor pages are excluded by not listing them.
  const guides = pages.filter(
    (p) =>
      p.slug !== 'index' &&
      (ladderScopes.length === 0 ||
        ladderScopes.some((s) => p.rel === s || p.rel.startsWith(`${s}/`))),
  );
  assert.ok(
    guides.length > 0,
    `--ladder matched no pages${ladderScopes.length ? ` under: ${ladderScopes.join(', ')}` : ''}`,
  );
  const missing = guides.filter((p) => {
    const body = p.body.toLowerCase();
    return !(body.includes('basic') && body.includes('everyday') && body.includes('advanced'));
  });
  assert.equal(
    missing.length,
    0,
    `pages missing floor-to-ceiling level labels (basics / Everyday / Advanced): ${missing
      .map((p) => p.rel)
      .join(', ')}`,
  );
}

// ---------------------------------------------------------------------------
console.log(`verify-docs: OK (${isHub ? 'hub' : 'content-only'}, ${pages.length} pages)`);
for (const p of pages) console.log(`  ${p.rel} — ${p.title}`);
