import { chromium } from 'playwright';
const BASE = process.env.WM_BASE_URL || 'http://localhost:3110';
const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
await page.goto(`${BASE}/index.html`, { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hex-svg"]').waitFor({ timeout: 30000 });
await page.waitForTimeout(500);

// Cycle to sorrys
for (let i = 0; i < 8; i++) {
  const label = ((await page.locator('[data-testid="view-toggle"]').textContent()) || '').replace('View: ', '').trim();
  if (label === 'sorrys') break;
  await page.locator('[data-testid="view-toggle"]').click();
  await page.waitForTimeout(300);
}

// Click the first sorry hex
const hex = page.locator('[data-testid="hex-svg"] g[data-node-id]').first();
const nodeId = await hex.getAttribute('data-node-id');
console.log('clicking sorry:', nodeId);
await hex.click();
await page.waitForTimeout(800);
await page.screenshot({ path: '/tmp/wm-detail-sorry.png', fullPage: true });

// Capture detail-panel text
const detail = await page.evaluate(() => {
  const panel = document.querySelector('[data-testid="detail-panel"]')
    || document.querySelector('.detail-panel')
    || document.querySelector('aside');
  if (panel) return panel.textContent.trim();
  // Fallback: anything that updated
  const status = document.querySelector('footer, .status-bar');
  return status ? status.textContent.trim() : '(no detail-panel found)';
});
console.log('detail-panel:', detail.slice(0, 1200));

// Also probe a stack hex
for (let i = 0; i < 8; i++) {
  const label = ((await page.locator('[data-testid="view-toggle"]').textContent()) || '').replace('View: ', '').trim();
  if (label === 'stack') break;
  await page.locator('[data-testid="view-toggle"]').click();
  await page.waitForTimeout(300);
}
const stackHex = page.locator('[data-testid="hex-svg"] g[data-node-id]').first();
const stackId = await stackHex.getAttribute('data-node-id');
console.log('clicking stack:', stackId);
await stackHex.click();
await page.waitForTimeout(800);
await page.screenshot({ path: '/tmp/wm-detail-stack.png', fullPage: true });
const detailStack = await page.evaluate(() => {
  const panel = document.querySelector('[data-testid="detail-panel"]')
    || document.querySelector('.detail-panel')
    || document.querySelector('aside');
  if (panel) return panel.textContent.trim();
  return '(no detail-panel found)';
});
console.log('detail-panel (stack):', detailStack.slice(0, 1200));

await browser.close();
