import { chromium } from 'playwright';
const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
const logs = [];
page.on('console', m => logs.push(`[${m.type()}] ${m.text()}`));
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hud"]').waitFor({ state: 'visible', timeout: 60000 });
await page.waitForTimeout(5000);

// Find all loop-arrow-* spans
const arrows = await page.locator('[data-testid^="loop-arrow-"]').all();
console.log('arrow span count:', arrows.length);
for (const a of arrows) {
  const tid = await a.getAttribute('data-testid');
  const t = await a.textContent();
  const title = await a.getAttribute('title');
  console.log(' ', tid, '->', JSON.stringify(t.trim()));
  console.log('     title:', title.slice(0, 130));
}

// Click each
for (const a of arrows) await a.click();
await page.waitForTimeout(300);
const anchorLogs = logs.filter(l => l.includes('[anchor wm-ui-anchor:0002]'));
console.log('\nclick logs:', anchorLogs.length);
for (const l of anchorLogs) console.log(' ', l);
await page.screenshot({ path: '/tmp/wm-anchor-0002-verify.png', fullPage: true });
await browser.close();
