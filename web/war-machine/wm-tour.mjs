// Standalone Playwright inspector — tour each view mode of the WM UI.
// Captures: screenshot per mode, view-toggle label, hex count, top-level
// DOM structure (h1/h2/h3 + data-testid attributes).
//
// Usage: node /tmp/wm-tour.mjs
//   WM_BASE_URL env var overrides the base (default: http://localhost:8710)

import { chromium } from 'playwright';

const BASE = process.env.WM_BASE_URL || 'http://localhost:8710';
const MODES = ['stack', 'aif-stack', 'missions', 'sorrys', 'invariants', 'patterns'];
const OUT = '/tmp/wm-tour';

async function snapshot(page, mode) {
  // Wait for SVG to be present
  await page.locator('[data-testid="hex-svg"]').waitFor({ state: 'visible', timeout: 20000 });
  await page.waitForTimeout(500); // settle reagent re-renders

  // Cell count
  const cellCount = await page.locator('[data-testid="hex-svg"] g[data-node-id]').count();

  // Headings + key testids
  const structure = await page.evaluate(() => {
    const headings = Array.from(document.querySelectorAll('h1,h2,h3'))
      .map(h => `${h.tagName.toLowerCase()}: ${h.textContent.trim().slice(0, 80)}`);
    const testids = Array.from(document.querySelectorAll('[data-testid]'))
      .map(e => e.getAttribute('data-testid'))
      .filter((v, i, a) => a.indexOf(v) === i);
    return { headings, testids };
  });

  // Pick one or two sample node-ids to show what's in this mode
  const sampleIds = await page.locator('[data-testid="hex-svg"] g[data-node-id]')
    .evaluateAll(els => els.slice(0, 5).map(e => e.getAttribute('data-node-id')));

  // Screenshot
  const file = `${OUT}-${mode}.png`;
  await page.screenshot({ path: file, fullPage: true });

  return { mode, cellCount, structure, sampleIds, screenshot: file };
}

async function cycleTo(page, target) {
  for (let i = 0; i < 8; i++) {
    const label = (await page.locator('[data-testid="view-toggle"]').textContent()) || '';
    if (label.replace('View: ', '').trim() === target) return true;
    await page.locator('[data-testid="view-toggle"]').click();
    await page.waitForTimeout(300);
  }
  return false;
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1400, height: 900 } });
  const page = await context.newPage();

  // Capture console messages for debugging
  const logs = [];
  page.on('console', msg => logs.push(`[${msg.type()}] ${msg.text()}`));
  page.on('pageerror', err => logs.push(`[ERR] ${err.message}`));

  console.log(`navigating to ${BASE}/index.html`);
  await page.goto(`${BASE}/index.html`, { waitUntil: 'domcontentloaded', timeout: 30000 });

  // Detail-panel content (selected node)
  const detail0 = await page.evaluate(() => {
    const panel = document.querySelector('[data-testid="detail-panel"], aside, .detail-panel');
    return panel ? panel.textContent.trim().slice(0, 400) : null;
  });

  const results = [];
  for (const mode of MODES) {
    const ok = await cycleTo(page, mode);
    if (!ok) {
      results.push({ mode, error: 'could not reach mode' });
      continue;
    }
    const snap = await snapshot(page, mode);
    results.push(snap);
  }

  console.log('\n=== TOUR SUMMARY ===\n');
  for (const r of results) {
    if (r.error) {
      console.log(`  ${r.mode}: ${r.error}`);
      continue;
    }
    console.log(`  ${r.mode}:`);
    console.log(`    cells: ${r.cellCount}`);
    console.log(`    sample-ids: ${r.sampleIds.join(', ')}`);
    console.log(`    headings: ${r.structure.headings.join(' | ')}`);
    console.log(`    testids: ${r.structure.testids.join(', ')}`);
    console.log(`    screenshot: ${r.screenshot}`);
    console.log('');
  }

  if (detail0) {
    console.log(`detail-panel-initial (first 400 chars):\n${detail0}\n`);
  }

  if (logs.length > 0) {
    console.log('=== CONSOLE / ERRORS (first 20) ===');
    logs.slice(0, 20).forEach(l => console.log(`  ${l}`));
  }

  await browser.close();
})().catch(e => { console.error(e); process.exit(1); });
