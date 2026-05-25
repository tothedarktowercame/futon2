// Verify anchor 0001 (consulting) + 0001a/b/c siblings (stack/mathematics/portfolio).
import { chromium } from 'playwright';
const ANCHORS = [
  { id: '0001',  ws: 'consulting',  testid: 'ws-pct-consulting',  hint: 'commits to consulting-workstream' },
  { id: '0001a', ws: 'stack',       testid: 'ws-pct-stack',       hint: 'commits to futon0-5 stack-workstream' },
  { id: '0001b', ws: 'mathematics', testid: 'ws-pct-mathematics', hint: 'primarily futon6' },
  { id: '0001c', ws: 'portfolio',   testid: 'ws-pct-portfolio',   hint: 'commits to portfolio-workstream' },
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
    title_first_100: title ? title.slice(0, 100) : null,
    has_pointer: style && style.includes('cursor: pointer'),
  };
}
console.log(JSON.stringify(results, null, 2));
for (const a of ANCHORS) {
  if (results[a.id].exists) {
    await page.locator(`[data-testid="${a.testid}"]`).click();
    await page.waitForTimeout(100);
  }
}
const anchorLogs = logs.filter(l => l.includes('[anchor wm-ui-anchor:'));
console.log('\nclick logs:', anchorLogs.length);
for (const l of anchorLogs) console.log(' ', l);
await page.screenshot({ path: '/tmp/wm-anchor-0001-verify.png', fullPage: true });
console.log('\nscreenshot /tmp/wm-anchor-0001-verify.png');
await browser.close();
