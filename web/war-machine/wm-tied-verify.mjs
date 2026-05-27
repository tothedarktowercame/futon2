import { chromium } from 'playwright';
const browser = await chromium.launch();
const page = await (await browser.newContext({ viewport: { width: 1400, height: 1100 } })).newPage();
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.waitForTimeout(5000);
const tied = page.locator('[data-testid="next-move-live-tied-bucket"]').first();
console.log('tied-bucket present?:', await tied.count());
if (await tied.count()) {
  const txt = await tied.textContent();
  console.log('tied bucket text:', (txt||'').slice(0,400));
}
await page.screenshot({ path: '/tmp/wm-tied.png', fullPage: false, clip: {x: 0, y: 80, width: 320, height: 480} });
await browser.close();
