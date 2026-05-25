import { chromium } from 'playwright';
const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
const logs = [];
page.on('console', m => logs.push(`[${m.type()}] ${m.text()}`));
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hud"]').waitFor({ state: 'visible', timeout: 60000 });
await page.waitForTimeout(5000);

const card = page.locator('[data-testid="next-move"]');
const cardExists = (await card.count()) > 0;
console.log('next-move card exists?', cardExists);

const tgt = page.locator('[data-testid="next-move-target"]');
const tgtExists = (await tgt.count()) > 0;
console.log('next-move-target exists?', tgtExists);

if (tgtExists) {
  console.log('target text:', (await tgt.textContent()).trim());
  const title = await tgt.getAttribute('title');
  console.log('title (first 250):', title.slice(0, 250));
  await tgt.click();
  await page.waitForTimeout(200);
}

const cardText = cardExists ? (await card.textContent()).slice(0, 400) : '';
console.log('\ncard text (first 400):', cardText);

const anchorLogs = logs.filter(l => l.includes('[anchor wm-ui-anchor:0009]'));
console.log('\nclick logs:', anchorLogs.length);
for (const l of anchorLogs) console.log(' ', l);

await page.screenshot({ path: '/tmp/wm-anchor-0009-verify.png', fullPage: true });
await browser.close();
