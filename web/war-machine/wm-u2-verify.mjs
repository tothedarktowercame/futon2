import { chromium } from 'playwright';
const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hud"]').waitFor({ state: 'visible', timeout: 60000 });
await page.waitForTimeout(5000);

const note = page.locator('[data-testid="waveform-bounds-note"]');
const exists = (await note.count()) > 0;
console.log('waveform-bounds-note exists?', exists);
if (exists) {
  const t = await note.textContent();
  console.log('text:', JSON.stringify(t.trim()));
}

// Sanity check the API window vs the displayed timeline bounds
const apiWin = await page.evaluate(async () => {
  const r = await fetch('http://localhost:7070/api/alpha/war-machine?days=14');
  const d = await r.json();
  return { days: d.window.days, start: d.window.start, end: d.window.end };
});
console.log('API window:', apiWin);

await page.screenshot({ path: '/tmp/wm-u2-verify.png', fullPage: true });
await browser.close();
