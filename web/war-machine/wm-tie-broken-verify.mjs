import { chromium } from 'playwright';
const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 1400, height: 1100 }, bypassCSP: true });
const page = await ctx.newPage();

// Force-bust cache via a no-cache query param
const url = 'http://localhost:3100/index.html?_t=' + Date.now();
await page.goto(url, { waitUntil: 'networkidle' });
await page.waitForTimeout(2500);

// Force a hard reload
await page.evaluate(() => location.reload(true));
await page.waitForTimeout(5000);

const header = page.locator('.next-move-tied-header').first();
const headerCount = await header.count();
console.log('tied-header count:', headerCount);
if (headerCount > 0) {
  const text = await header.textContent();
  console.log('header text:', (text||'').replace(/\s+/g,' ').slice(0, 300));
}

const recommend = page.locator('[data-testid="next-move-tied-top-recommend"]').first();
const recCount = await recommend.count();
console.log('recommend block count:', recCount);
if (recCount > 0) {
  console.log('recommend text:', (await recommend.textContent()||'').slice(0, 200));
}

// Check main.js URL/hash on the page
const scripts = await page.evaluate(() => Array.from(document.scripts).map(s => s.src));
console.log('script urls:', scripts.filter(s => s.includes('main')).slice(0, 3));

await page.screenshot({ path: '/tmp/wm-tie-broken.png', fullPage: false, clip: {x: 0, y: 0, width: 700, height: 700} });
console.log('screenshot: /tmp/wm-tie-broken.png');
await browser.close();
