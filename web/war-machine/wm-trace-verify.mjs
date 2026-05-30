import { chromium } from 'playwright';
const browser = await chromium.launch();
const page = await (await browser.newContext({ viewport: { width: 1400, height: 1100 } })).newPage();
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.waitForTimeout(5000);

const tied = page.locator('[data-testid="next-move-live-tied-bucket"]').first();
const tiedCount = await tied.count();
console.log('tied-bucket present?:', tiedCount);

const traces = page.locator('details.next-move-trace');
const nTraces = await traces.count();
console.log('next-move-trace <details> count:', nTraces);

if (nTraces > 0) {
  // Open all of them
  for (let i = 0; i < nTraces; i++) {
    const d = traces.nth(i);
    await d.evaluate(el => el.open = true);
  }
  await page.waitForTimeout(200);
  // Read first opened trace
  const first = traces.first();
  const text = await first.textContent();
  console.log('first trace text:', (text || '').slice(0, 600));
}

await page.screenshot({ path: '/tmp/wm-trace.png', fullPage: false, clip: {x: 0, y: 0, width: 700, height: 900} });
console.log('screenshot saved: /tmp/wm-trace.png');
await browser.close();
