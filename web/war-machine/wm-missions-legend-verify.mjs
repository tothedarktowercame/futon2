import { chromium } from 'playwright';
const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 1900, height: 1200 } });
const page = await ctx.newPage();
const url = 'http://localhost:3100/index.html?_t=' + Date.now();
await page.goto(url, { waitUntil: 'networkidle' });
await page.waitForTimeout(3000);

// Switch to missions view
// Cycle the view-toggle through view modes until missions is shown
const viewToggle = page.locator('[data-testid="view-toggle"]').first();
for (let i = 0; i < 7; i++) {
  const label = (await viewToggle.textContent() || '').trim();
  if (/missions/i.test(label)) break;
  await viewToggle.click();
  await page.waitForTimeout(400);
}
await page.waitForTimeout(1500);

// Read legend swatches (background-color computed style)
const swatches = await page.locator('.legend-card .row').evaluateAll((rows) =>
  rows.map(r => {
    const swatch = r.querySelector('.swatch');
    const label = r.querySelector('span');
    if (!swatch || !label) return null;
    return {
      bg: getComputedStyle(swatch).backgroundColor,
      border: getComputedStyle(swatch).borderColor,
      label: label.textContent.trim()
    };
  }).filter(Boolean)
);
console.log('legend swatches:');
for (const s of swatches) console.log(' ', s.label, '→', s.bg);

// Read hex sizes
const hexSizes = await page.locator('svg[data-testid="hex-svg"] polygon').evaluateAll(els =>
  els.slice(0, 5).map(p => {
    const pts = p.getAttribute('points');
    if (!pts) return null;
    const coords = pts.split(/\s+/).map(s => s.split(',').map(Number)).filter(c => c.length === 2);
    if (coords.length < 2) return null;
    const xs = coords.map(c => c[0]);
    const span = Math.max(...xs) - Math.min(...xs);
    return Math.round(span);
  }).filter(Boolean)
);
console.log('first-5 hex x-spans (px):', hexSizes);

await page.screenshot({ path: '/tmp/wm-missions-legend.png', clip: {x: 0, y: 0, width: 1900, height: 900} });
console.log('screenshot: /tmp/wm-missions-legend.png');
await browser.close();
