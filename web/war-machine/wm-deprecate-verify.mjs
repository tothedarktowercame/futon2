import { chromium } from 'playwright';
const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hud"]').waitFor({ state: 'visible', timeout: 120000 });
await page.waitForTimeout(5000);

// Cycle through, collecting labels
const seen = new Set();
for (let i = 0; i < 10; i++) {
  const label = ((await page.locator('[data-testid="view-toggle"]').textContent()) || '').replace('View: ', '').trim();
  if (seen.has(label)) break;
  seen.add(label);
  await page.locator('[data-testid="view-toggle"]').click();
  await page.waitForTimeout(250);
}
console.log('view-modes reachable via toggle:', [...seen]);
console.log('sorrys still reachable?', seen.has('sorrys') ? 'YES (bug)' : 'no — deleted');
console.log('invariants still reachable?', seen.has('invariants') ? 'YES (bug)' : 'no — deleted');
await browser.close();
