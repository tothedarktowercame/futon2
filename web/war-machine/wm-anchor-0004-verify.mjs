// Anchor 0004 verification: HUD Mode span carries tooltip + click handler.
import { chromium } from 'playwright';
const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
const consoleLogs = [];
page.on('console', m => consoleLogs.push(`[${m.type()}] ${m.text()}`));
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.locator('[data-testid="hud"]').waitFor({ state: 'visible', timeout: 60000 });
await page.waitForTimeout(5000); // let data load

// (a) [data-testid="hud-mode"] exists
const modeSpan = page.locator('[data-testid="hud-mode"]');
const exists = await modeSpan.count();
console.log('hud-mode exists?', exists > 0);

// (b) text matches API judgement.mode
const modeText = (await modeSpan.textContent() || '').trim();
console.log('mode span text:', JSON.stringify(modeText));
const apiMode = await page.evaluate(async () => {
  const r = await fetch('http://localhost:7070/api/alpha/war-machine?days=14');
  const d = await r.json();
  return d['judgement'] && d['judgement']['mode'];
});
console.log('API judgement.mode:', JSON.stringify(apiMode));
console.log('text-matches-api?', modeText.startsWith(apiMode));

// (c) title attribute contains the rationale
const titleAttr = await modeSpan.getAttribute('title');
console.log('title attr:', titleAttr);
const hasRationale = titleAttr && titleAttr.includes('threshold') && titleAttr.includes('Click');
console.log('title-has-rationale?', hasRationale);

// (d) cursor:pointer style
const style = await modeSpan.getAttribute('style');
console.log('inline style:', style);

// (e) clicking it produces the expected console log
await modeSpan.click();
await page.waitForTimeout(300);
const matched = consoleLogs.filter(l => l.includes('wm-ui-anchor:0004'));
console.log('on-click console logs:', matched.length, matched[0] || '(none)');

await page.screenshot({ path: '/tmp/wm-anchor-0004-verify.png', fullPage: true });
console.log('screenshot /tmp/wm-anchor-0004-verify.png');

// Hover screenshot — to see the tooltip
await modeSpan.hover();
await page.waitForTimeout(800);
await page.screenshot({ path: '/tmp/wm-anchor-0004-hover.png', fullPage: true });
console.log('hover screenshot /tmp/wm-anchor-0004-hover.png');

await browser.close();
