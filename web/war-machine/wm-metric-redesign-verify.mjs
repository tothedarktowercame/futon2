import { chromium } from 'playwright';
const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
page.on('pageerror', e => console.log('[pageerror]', e.message.slice(0,300)));
await page.goto('http://localhost:8710/index.html', { waitUntil: 'networkidle' });
await page.waitForTimeout(3500);

// Loop health: extract rendered percentages by data-testid
const arrows = ['work→proof','proof→patterns','patterns→coordination','coordination→self-rep','self-rep→inference','inference→work'];
const results = {};
for (const a of arrows) {
  const el = page.locator(`[data-testid="loop-health-arrow-${a}"]`).first();
  if (await el.count()) results[a] = (await el.textContent())?.trim().slice(0,40);
}

// Generic fallback: grab the loop-health card text
const card = page.locator('text=Loop Health').locator('..').first();
const cardText = (await card.textContent())?.slice(0, 800);

console.log(JSON.stringify({ per_arrow_testid_text: results, loop_health_card_excerpt: cardText }, null, 2));
await page.screenshot({ path: '/tmp/wm-metrics.png', fullPage: false, clip: { x: 1050, y: 100, width: 350, height: 350 } });
await browser.close();
