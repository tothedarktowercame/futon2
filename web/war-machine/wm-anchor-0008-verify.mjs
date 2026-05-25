import { chromium } from 'playwright';
const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
const logs = [];
page.on('console', m => logs.push(`[${m.type()}] ${m.text()}`));
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hud"]').waitFor({ state: 'visible', timeout: 60000 });
await page.waitForTimeout(5000);

const tile = page.locator('[data-testid="pocketwatch-warnings"]');
const exists = (await tile.count()) > 0;
console.log('pocketwatch-warnings exists?', exists);

if (exists) {
  const text = await tile.textContent();
  console.log('tile text (first 600 chars):', text.slice(0, 600));
}

const cards = await page.locator('[data-testid^="warning-card-"]').all();
console.log('warning cards:', cards.length);
for (const c of cards) {
  const tid = await c.getAttribute('data-testid');
  const title = await c.getAttribute('title');
  console.log(' ', tid);
  console.log('     title:', title ? title.slice(0, 120) : '(no title)');
  await c.click();
  await page.waitForTimeout(150);
}

const anchorLogs = logs.filter(l => l.includes('[anchor wm-ui-anchor:0008]'));
console.log('\nclick logs:', anchorLogs.length);
for (const l of anchorLogs) console.log(' ', l);

await page.screenshot({ path: '/tmp/wm-anchor-0008-verify.png', fullPage: true });
await browser.close();
