// Verify the single-JVM path: shadow-cljs (8710) proxies /api to futon3c (7070).
// Wait long enough (60s) for the war-machine response.
import { chromium } from 'playwright';

const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();

const logs = [];
page.on('console', m => logs.push(`[${m.type()}] ${m.text()}`));
page.on('pageerror', e => logs.push(`[PAGEERR] ${e.message}`));

console.log('navigating http://localhost:8710/index.html');
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded', timeout: 30000 });

// Wait up to 60s for the hex SVG to appear with cells
console.log('waiting for hex-svg to render (up to 60s)...');
try {
  await page.locator('[data-testid="hex-svg"]').waitFor({ state: 'visible', timeout: 60000 });
  await page.waitForFunction(() => {
    const cells = document.querySelectorAll('[data-testid="hex-svg"] g[data-node-id]');
    return cells.length > 0;
  }, { timeout: 60000 });
  console.log('OK hex-svg rendered with cells');
} catch (e) {
  console.log('FAIL', e.message);
}

const body = await page.evaluate(() => document.body.innerText.slice(0, 800));
const stillLoading = body.includes('Loading War Machine');
console.log('still Loading?', stillLoading);

const cellCount = await page.locator('[data-testid="hex-svg"] g[data-node-id]').count();
console.log('cell count:', cellCount);

const status = await page.evaluate(() => {
  const txt = document.body.innerText;
  const m = txt.match(/Mode\s+(\w+)/);
  return m ? m[1] : '(no Mode line)';
});
console.log('status-bar Mode:', status);

await page.screenshot({ path: '/tmp/wm-singlejvm-verify.png', fullPage: true });
console.log('screenshot /tmp/wm-singlejvm-verify.png');

console.log('\n=== last 20 console messages ===');
logs.slice(-20).forEach(l => console.log(' ', l));

await browser.close();
