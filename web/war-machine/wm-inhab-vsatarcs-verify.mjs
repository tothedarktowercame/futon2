import { chromium } from 'playwright';
const browser = await chromium.launch();
const page = await (await browser.newContext({ viewport: { width: 1400, height: 1200 } })).newPage();
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded', timeout: 30000 });
await page.waitForTimeout(5000);

// Click view-selector button repeatedly until it shows "View: self-watch"
const viewBtn = page.locator('button:has-text("View:")').first();
for (let i = 0; i < 8; i++) {
  const t = await viewBtn.textContent();
  console.log('view:', t);
  if (t && t.toLowerCase().includes('self-watch')) break;
  await viewBtn.click();
  await page.waitForTimeout(400);
}

await page.waitForTimeout(800);
const wipSum = page.locator('[data-testid="inhabitation-log-wip"] summary').first();
const doneSum = page.locator('[data-testid="inhabitation-log-done"] summary').first();
console.log('wipCount:', await wipSum.count(), 'doneCount:', await doneSum.count());
if (await wipSum.count()) await wipSum.click({timeout:5000}).catch(()=>{});
if (await doneSum.count()) await doneSum.click({timeout:5000}).catch(()=>{});
await page.waitForTimeout(500);

console.log('VSATARCS ✓:', await page.locator('text=/VSATARCS ✓/').count());
console.log('VSATARCS ○:', await page.locator('text=/VSATARCS ○/').count());
const dt = (await page.locator('[data-testid="inhabitation-log-done"]').textContent()) || '';
console.log('done excerpt:', dt.slice(0,500));

await page.screenshot({ path: '/tmp/wm-inhab.png', fullPage: true });
await browser.close();
