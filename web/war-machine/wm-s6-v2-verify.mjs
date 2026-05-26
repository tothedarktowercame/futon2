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
  if (t && t.includes('Cached prior')) { await s.click(); break; }
}
await page.waitForTimeout(500);

const txt = async (sel) => {
  const el = page.locator(sel).first();
  return (await el.count()) ? (await el.textContent())?.trim().slice(0,180) : null;
};

console.log(JSON.stringify({
  phaseComplete: await txt('.next-move-phase-complete'),
  phaseNext: await txt('.next-move-phase:not(.next-move-phase-complete)'),
  rollover: await txt('.next-move-rollover'),
  effect: await txt('.next-move-effect'),
  successor: await txt('.next-move-successor'),
  // The "underspecified" warning should be gone
  underspecified: await page.locator('.next-move-underspecified').count(),
  missing: await page.locator('.next-move-missing').count(),
  // Look for "still missing" text
  stillMissing: await page.getByText(/still missing/i).count()
}, null, 2));

await page.screenshot({ path: '/tmp/wm-s6-v2.png', fullPage: false });
await browser.close();
