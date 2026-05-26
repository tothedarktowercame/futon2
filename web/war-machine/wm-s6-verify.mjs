import { chromium } from 'playwright';
const browser = await chromium.launch();
const ctx = await browser.newContext();
const page = await ctx.newPage();
page.on('pageerror', e => console.log('[pageerror]', e.message.slice(0,300)));
await page.goto('http://localhost:8710/index.html', { waitUntil: 'networkidle' });
await page.waitForTimeout(3500);

// Expand the cached-prior <details>
const summaries = await page.locator('details summary').all();
for (const s of summaries) {
  const t = await s.textContent();
  if (t && t.includes('Cached prior')) {
    await s.click();
    break;
  }
}
await page.waitForTimeout(500);

const phaseComplete = await page.locator('.next-move-phase-complete').first();
const phaseNext = await page.locator('.next-move-phase').nth(1);
const rollover = await page.locator('.next-move-rollover').first();
const effect = await page.locator('.next-move-effect').first();
const witness = await page.locator('.next-move-witness').first();

const txt = async (el) => (await el.count()) ? (await el.textContent())?.trim().slice(0,200) : null;

console.log(JSON.stringify({
  liveBadge: await txt(page.locator('.wm-live-badge').first()),
  phaseComplete: await txt(phaseComplete),
  rollover: await txt(rollover),
  effect: await txt(effect),
  witness: await txt(witness)
}, null, 2));

await page.screenshot({ path: '/tmp/wm-s6.png', fullPage: false });
console.log('screenshot:/tmp/wm-s6.png');
await browser.close();
