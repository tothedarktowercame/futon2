import { chromium } from 'playwright';
const browser = await chromium.launch();
const ctx = await browser.newContext();
const page = await ctx.newPage();
page.on('console', m => console.log('[console]', m.type(), m.text().slice(0,250)));
page.on('pageerror', e => console.log('[pageerror]', e.message.slice(0,500)));
await page.goto('http://localhost:8710/index.html', { waitUntil: 'networkidle' });
await page.waitForTimeout(3500);
const liveTile = await page.locator('[data-testid="next-move-live"]').count();
const cachedFallback = await page.locator('[data-testid="next-move"]').count();
const liveTargetEl = page.locator('[data-testid="next-move-live-target"]').first();
const liveTargetText = await liveTargetEl.count() ? await liveTargetEl.textContent() : null;
const badgeEls = await page.locator('.wm-live-badge').all();
const badgeText = badgeEls.length ? await badgeEls[0].textContent() : null;
const freshnessWarn = await page.locator('.next-move-freshness-warning').count();
console.log(JSON.stringify({
  liveTileCount: liveTile,
  cachedTileCount: cachedFallback,
  liveTargetText: liveTargetText?.slice(0,150),
  badgeText,
  freshnessWarnCount: freshnessWarn
}, null, 2));
await page.screenshot({ path: '/tmp/wm-live-rec.png', fullPage: true });
console.log('screenshot:/tmp/wm-live-rec.png');
await browser.close();
