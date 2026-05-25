import { chromium } from 'playwright';
const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
const logs = [];
page.on('console', m => logs.push(`[${m.type()}] ${m.text()}`));
page.on('pageerror', e => logs.push(`[PAGEERR] ${e.message}`));
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hex-svg"]').waitFor({ state: 'visible', timeout: 120000 });
await page.waitForFunction(() => document.querySelectorAll('[data-testid="hex-svg"] g[data-node-id]').length > 0, { timeout: 120000 });

async function modeLabel() {
  return ((await page.locator('[data-testid="view-toggle"]').textContent()) || '').replace('View: ', '').trim();
}
async function snapshot(label) {
  const ids = await page.locator('[data-testid="hex-svg"] g[data-node-id]').evaluateAll(els => els.map(e => e.getAttribute('data-node-id')));
  const counts = {};
  for (const id of ids) counts[id] = (counts[id] || 0) + 1;
  const dupes = Object.entries(counts).filter(([_, n]) => n > 1).map(([id, n]) => `${id}×${n}`);
  console.log(`${label}: total=${ids.length} unique=${Object.keys(counts).length} dupes=${dupes.length}${dupes.length ? ' [' + dupes.slice(0,5).join(', ') + ']' : ''}`);
  return { total: ids.length, unique: Object.keys(counts).length, dupes };
}

console.log('=== first pass: visit each mode once ===');
const MODES = ['stack', 'self-watch', 'aif-stack', 'missions', 'sorrys', 'invariants', 'patterns'];
const first = {};
for (const m of MODES) {
  let safety = 10;
  while ((await modeLabel()) !== m && safety-- > 0) {
    await page.locator('[data-testid="view-toggle"]').click();
    await page.waitForTimeout(250);
  }
  await page.waitForTimeout(500);
  first[m] = await snapshot(m);
}

console.log('\n=== second pass after full cycle ===');
for (let i = 0; i < MODES.length; i++) {
  await page.locator('[data-testid="view-toggle"]').click();
  await page.waitForTimeout(250);
}
for (const m of MODES) {
  let safety = 10;
  while ((await modeLabel()) !== m && safety-- > 0) {
    await page.locator('[data-testid="view-toggle"]').click();
    await page.waitForTimeout(250);
  }
  await page.waitForTimeout(500);
  const snap = await snapshot(m);
  const drift = snap.total - first[m].total;
  if (drift) console.log(`  DRIFT in ${m}: ${first[m].total} → ${snap.total} (+${drift} ghost cells)`);
}

await page.screenshot({ path: '/tmp/wm-ghost-repro.png', fullPage: true });
