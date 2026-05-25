import { chromium } from 'playwright';
const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
const logs = [];
page.on('console', m => logs.push(`[${m.type()}] ${m.text()}`));
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hud"]').waitFor({ state: 'visible', timeout: 60000 });
await page.waitForTimeout(5000);
// Cycle to self-watch view (stack → self-watch is the first toggle click)
for (let i = 0; i < 8; i++) {
  const label = ((await page.locator('[data-testid="view-toggle"]').textContent()) || '').replace('View: ', '').trim();
  if (label === 'self-watch') break;
  await page.locator('[data-testid="view-toggle"]').click();
  await page.waitForTimeout(300);
}
await page.locator('[data-testid="self-watch-dashboard"]').waitFor({ state: 'visible', timeout: 10000 });
const counter = page.locator('[data-testid="self-watch-issue-count"]');
const exists = (await counter.count()) > 0;
console.log('self-watch-issue-count exists?', exists);
if (exists) {
  console.log('counter text:', JSON.stringify((await counter.textContent()).trim()));
  console.log('title first 200:', (await counter.getAttribute('title')).slice(0, 200));
  await counter.click();
  await page.waitForTimeout(150);
}
// Hand-off buttons per issue
const buttons = await page.locator('[data-testid^="self-watch-handoff-"]').all();
console.log('hand-off buttons:', buttons.length);
const anchorLogs = logs.filter(l => l.includes('[anchor wm-ui-anchor:0010]'));
console.log('click logs:', anchorLogs.length);
for (const l of anchorLogs) console.log(' ', l);
await page.screenshot({ path: '/tmp/wm-anchor-0010-verify.png', fullPage: true });
await browser.close();
