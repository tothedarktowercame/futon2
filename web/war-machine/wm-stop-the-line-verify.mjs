import { chromium } from 'playwright';
const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
const logs = [];
page.on('console', m => logs.push(`[${m.type()}] ${m.text()}`));
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hud"]').waitFor({ state: 'visible', timeout: 60000 });
await page.waitForTimeout(5000);

// Banner must appear in EVERY view per :μ/override-modes semantics.
// Test it on the default (stack) view first, then again after switching to self-watch.
const banner = page.locator('[data-testid="stop-the-line-banner"]');
const bannerCountStack = await banner.count();
console.log('banner-on-stack-view count:', bannerCountStack);
if (bannerCountStack > 0) {
  const txt = (await banner.textContent()).replace(/\s+/g, ' ').trim();
  console.log('banner-text:', JSON.stringify(txt.slice(0, 200)));
  const isVisible = await banner.isVisible();
  console.log('banner-visible:', isVisible);
  const box = await banner.boundingBox();
  console.log('banner-box:', JSON.stringify(box));
  // Confirm it sits ABOVE main-area (smaller y).
  const mainArea = page.locator('.main-area');
  if (await mainArea.count() > 0) {
    const mainBox = await mainArea.boundingBox();
    console.log('main-area-y:', mainBox && mainBox.y);
    console.log('banner-above-main?', box && mainBox && box.y < mainBox.y);
  }
}

// Cycle to self-watch view, confirm banner still present.
for (let i = 0; i < 8; i++) {
  const label = ((await page.locator('[data-testid="view-toggle"]').textContent()) || '').replace('View: ', '').trim();
  if (label === 'self-watch') break;
  await page.locator('[data-testid="view-toggle"]').click();
  await page.waitForTimeout(300);
}
const bannerCountSelfWatch = await page.locator('[data-testid="stop-the-line-banner"]').count();
console.log('banner-on-self-watch-view count:', bannerCountSelfWatch);

// Confirm Pilot Contract card is gone, Inhabitation Log is present.
const pilotContractMatches = await page.locator('text=Pilot Contract').count();
const inhabitationLogMatches = await page.locator('text=Inhabitation Log').count();
console.log('Pilot-Contract-text-present (should be 0):', pilotContractMatches);
console.log('Inhabitation-Log-text-present (should be >=1):', inhabitationLogMatches);

// Click [work in progress] tooltip and assert it expands.
const wip = page.locator('[data-testid="inhabitation-log-wip"]');
if ((await wip.count()) > 0) {
  await wip.locator('summary').click();
  await page.waitForTimeout(200);
  const isOpen = await wip.evaluate(el => el.open);
  console.log('inhabitation-log-wip-expanded:', isOpen);
  const detailText = (await wip.textContent()).replace(/\s+/g, ' ').trim();
  console.log('wip-text-includes-cg-id?', detailText.includes('cg-'));
  console.log('wip-text-includes-anchor-0011?', detailText.includes('wm-ui-anchor:0011'));
}

await page.screenshot({ path: '/tmp/wm-stop-the-line-verify.png', fullPage: true });
console.log('screenshot: /tmp/wm-stop-the-line-verify.png');
await browser.close();
