// Verify HUD cluster anchors 0003/0005/0006/0007 + 0004 unchanged.
import { chromium } from 'playwright';

const ANCHORS = [
  { id: '0003', testid: 'hud-loop',     hint: 'aggregate of the 6 holistic-argument' },
  { id: '0004', testid: 'hud-mode',     hint: 'Click for the 6-mode taxonomy' },
  { id: '0005', testid: 'hud-ants',     hint: 'cyberants-template' },
  { id: '0006', testid: 'hud-missions', hint: 'mission-health' },
  { id: '0007', testid: 'hud-sorrys',   hint: 'registry-split is tracked' },
];

const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
const logs = [];
page.on('console', m => logs.push(`[${m.type()}] ${m.text()}`));

await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hud"]').waitFor({ state: 'visible', timeout: 60000 });
await page.waitForTimeout(5000);

const results = {};
for (const a of ANCHORS) {
  const span = page.locator(`[data-testid="${a.testid}"]`);
  const exists = (await span.count()) > 0;
  const text = exists ? ((await span.textContent()) || '').trim() : '';
  const title = exists ? (await span.getAttribute('title')) : null;
  const style = exists ? (await span.getAttribute('style')) : null;
  results[a.id] = {
    testid: a.testid,
    exists,
    text,
    title_has_hint: title && title.includes(a.hint),
    title_first_120: title ? title.slice(0, 120) : null,
    has_pointer: style && style.includes('cursor: pointer'),
  };
}

console.log(JSON.stringify(results, null, 2));

// Click each span; collect anchor-logs
for (const a of ANCHORS) {
  await page.locator(`[data-testid="${a.testid}"]`).click();
  await page.waitForTimeout(150);
}
const anchorLogs = logs.filter(l => l.includes('[anchor wm-ui-anchor:'));
console.log('\nanchor click logs:', anchorLogs.length);
for (const l of anchorLogs) console.log(' ', l);

await page.screenshot({ path: '/tmp/wm-hud-cluster-verify.png', fullPage: true });
console.log('\nscreenshot /tmp/wm-hud-cluster-verify.png');

await browser.close();
