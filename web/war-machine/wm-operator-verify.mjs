import { chromium } from 'playwright';
const hits = [], status = {};
const browser = await chromium.launch();
const page = await (await browser.newContext({ viewport: { width: 1500, height: 1200 } })).newPage();
page.on('request', r => { const u=r.url(); if (u.includes('operator-bulletin')||u.includes('forward-model')) hits.push('/api'+(u.split('/api')[1]||u)); });
page.on('response', r => { const u=r.url(); if(u.includes('operator-bulletin')) status['operator-bulletin']=r.status(); if(u.includes('forward-model')) status['forward-model']=r.status(); });
await page.goto('http://localhost:8710/index.html', { waitUntil: 'domcontentloaded' });
await page.waitForTimeout(2500);
const toggle = page.locator('[data-testid="view-toggle"]').first();
let reached = false;
for (let i=0;i<7;i++){
  const label = (await toggle.textContent().catch(()=>''))||'';
  if (/operator/i.test(label)) { reached = true; break; }
  await toggle.click().catch(()=>{});
  await page.waitForTimeout(600);
}
await page.waitForTimeout(4000);
const dash = page.locator('[data-testid="operator-dashboard"]').first();
console.log('reached operator view?:', reached, '| dashboard present?:', await dash.count());
console.log('ENDPOINT HITS:', JSON.stringify([...new Set(hits)]));
console.log('RESPONSE STATUS:', JSON.stringify(status));
const dashTxt = (await dash.innerText().catch(()=>'')) || '';
console.log('OPERATOR DASHBOARD TEXT (first 1400):\n' + dashTxt.slice(0,1400));
await page.screenshot({ path: '/tmp/wm-operator.png', fullPage: true });
await browser.close();
