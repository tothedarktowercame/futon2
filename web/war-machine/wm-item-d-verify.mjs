// Item D verification: status bar should show "Mode <inferred-mode>" not "Mode ?".
import { chromium } from 'playwright';

const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
const logs = [];
page.on('console', m => logs.push(`[${m.type()}] ${m.text()}`));

await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hud"]').waitFor({ state: 'visible', timeout: 60000 });

// Wait until the hud line has either a real mode word or stays at "?"; the
// data load takes ~30s on cold cache so poll for the change.
let modeText = '?';
for (let i = 0; i < 60; i++) {
  modeText = await page.evaluate(() => {
    const hud = document.querySelector('[data-testid="hud"]');
    if (!hud) return '(no-hud)';
    const m = hud.textContent.match(/Mode\s+(\S+)/);
    return m ? m[1] : '(no-mode-match)';
  });
  if (modeText !== '?' && modeText !== '(no-mode-match)' && modeText !== '(no-hud)') break;
  await page.waitForTimeout(1000);
}

const hudText = await page.locator('[data-testid="hud"]').textContent();
console.log('HUD text:', hudText);
console.log('Extracted mode:', modeText);

// Cross-check: the value should match futon3c's judgement.mode
const apiCheck = await page.evaluate(async () => {
  const resp = await fetch('http://localhost:7070/api/alpha/war-machine?days=14');
  const data = await resp.json();
  return data['judgement'] && data['judgement']['mode'];
});
console.log('futon3c judgement.mode:', apiCheck);
console.log('match?', String(modeText) === String(apiCheck) ? 'YES' : 'NO');

// Cycle through view modes; mode value should stay the same across them
const modes = ['aif-stack', 'missions', 'sorrys', 'invariants', 'patterns', 'stack'];
const perView = {};
for (const m of modes) {
  for (let i = 0; i < 8; i++) {
    const label = ((await page.locator('[data-testid="view-toggle"]').textContent()) || '').replace('View: ', '').trim();
    if (label === m) break;
    await page.locator('[data-testid="view-toggle"]').click();
    await page.waitForTimeout(200);
  }
  const t = await page.locator('[data-testid="hud"]').textContent();
  const mm = t.match(/Mode\s+(\S+)/);
  perView[m] = mm ? mm[1] : '?';
}
console.log('mode per view-mode:', JSON.stringify(perView));

await page.screenshot({ path: '/tmp/wm-item-d-verify.png', fullPage: true });
console.log('screenshot /tmp/wm-item-d-verify.png');
await browser.close();
