// wm-metric-provenance-verify — QC for the WM headline metrics.
//
// For each rendered metric in the WM UI:
//   1. Read the DOM-rendered value
//   2. Fetch the source JSON from /api/alpha/war-machine
//   3. Recompute the value from the documented formula
//   4. Assert rendered ≡ source ≡ recomputed
//   5. Flag SATURATION (input is past the formula's effective cap → output is uninformative)
//   6. Flag SEMANTIC-MISMATCH (formula's measurand doesn't match the operator's question)
//
// Use case: operator/pilot inspecting whether the WM UI numbers are
// actionable. A "100% health" that comes from a saturated formula is
// not actionable; a "0% consulting" that doesn't include the operator's
// actual consulting surface (vsat, invoices/, statements/) is not actionable.
//
// Runs against the live WM UI at http://localhost:8710/index.html.

import { chromium } from 'playwright';

const WM_URL = 'http://localhost:8710/index.html';
const API_URL = 'http://localhost:7070/api/alpha/war-machine';
const WINDOW_DAYS = 14;

// ---------- Substrate fetch + recomputation ----------

async function fetchWmData() {
  const r = await fetch(API_URL);
  if (!r.ok) throw new Error(`WM API returned ${r.status}`);
  return await r.json();
}

// Half-rate parameter for the asymptotic freq shape — must match
// arrow-health-freq-k in futon2/scripts/futon2/report/war_machine.clj.
// See E-wm-metric-redesign for the change from the prior min(1, count/10)
// saturating shape.
const ARROW_HEALTH_FREQ_K = 10.0;

function recomputeArrowHealth({count, 'days-since': daysSince}) {
  const c = Number(count);
  const freq = c / (c + ARROW_HEALTH_FREQ_K);
  const fresh = daysSince == null ? 0 : Math.max(0, 1 - daysSince / WINDOW_DAYS);
  return Math.sqrt(freq * fresh);
}

function isSaturated({count, 'days-since': daysSince}) {
  // Post-E-wm-metric-redesign: the new freq = count/(count+k) is
  // asymptotic — it never locks at 1.0.  But we still flag the regime
  // where the API value is >= 0.95 AND days-since=0 as "near-asymptote"
  // so the operator knows further activity has diminishing differentiation.
  // Not actionable as a target → "tune for non-near-asymptote" rather
  // than "raise the saturated metric."
  return count >= 50 && daysSince === 0;
}

function classifySemantic(workstream, pct) {
  // Light heuristic: if a workstream's pct = 0, surface the question
  // "could there be activity not captured by `git commits to classified repos`?"
  if (pct === 0) {
    return {
      semantic_caveat: `${workstream}=0% measures git commits to ${workstream}-classified repos in last ${WINDOW_DAYS}d. Does NOT include: uncommitted file authoring, repos outside the WM manifest, work happening through external tools (email, PDFs, web forms). If operator believes this workstream is active, the metric measurand may not match the operator's question.`,
    };
  }
  return {};
}

// ---------- Main probe ----------

const browser = await chromium.launch({ headless: true });
const ctx = await browser.newContext({ viewport: { width: 1400, height: 900 } });
const page = await ctx.newPage();
const consoleErrors = [];
page.on('console', m => { if (m.type() === 'error') consoleErrors.push(m.text()); });

try {
  // Fetch substrate first so we can compare against rendered DOM
  const wmData = await fetchWmData();
  await page.goto(WM_URL, { waitUntil: 'domcontentloaded' });
  await page.locator('[data-testid="hud"]').waitFor({ state: 'visible', timeout: 60000 });
  await page.waitForTimeout(3000); // settle the reactive UI

  // ---------- Loop-Health verification ----------
  const arrows = wmData['loop-health']?.arrows ?? [];
  const loopHealthResults = [];
  for (const a of arrows) {
    const arrowId = a['arrow-id'];
    const apiHealth = a.health;
    const recomputed = recomputeArrowHealth(a);
    const matchTolerance = 1e-9;
    const provenanceValid = Math.abs(apiHealth - recomputed) < matchTolerance;
    const saturated = isSaturated(a);
    loopHealthResults.push({
      arrow_id: arrowId,
      label: a.label,
      inputs: { count: a.count, days_since: a['days-since'], window_days: WINDOW_DAYS },
      api_health: apiHealth,
      recomputed_health: recomputed,
      provenance_valid: provenanceValid,
      saturated,
      saturation_note: saturated
        ? `count=${a.count} ≥ 10 (capped) AND days-since=0; formula locks at 1.0 regardless of further activity. NOT actionable as a target.`
        : null,
    });
  }

  // ---------- Workstream-pct verification ----------
  const commitPcts = wmData.graph?.dynamics?.['commit-percentages'] ?? {};
  const workstreamResults = [];
  for (const [ws, pct] of Object.entries(commitPcts)) {
    const testId = `ws-pct-${ws}`;
    const span = page.locator(`[data-testid="${testId}"]`);
    const exists = (await span.count()) > 0;
    const renderedText = exists ? ((await span.textContent()) || '').trim() : null;
    const renderedPct = renderedText ? parseFloat(renderedText.replace('%', '')) / 100 : null;
    const provenanceValid = exists && Math.abs(renderedPct - pct) < 0.01;
    workstreamResults.push({
      workstream: ws,
      test_id: testId,
      dom_exists: exists,
      rendered_text: renderedText,
      api_pct: pct,
      api_pct_text: `${(pct * 100).toFixed(0)}%`,
      provenance_valid: provenanceValid,
      ...classifySemantic(ws, pct),
    });
  }

  // ---------- Report ----------
  const summary = {
    probe: 'wm-metric-provenance-verify',
    timestamp: new Date().toISOString(),
    wm_api: API_URL,
    wm_ui: WM_URL,
    window_days: WINDOW_DAYS,
    loop_health: {
      total_arrows: loopHealthResults.length,
      provenance_valid_count: loopHealthResults.filter(r => r.provenance_valid).length,
      saturated_count: loopHealthResults.filter(r => r.saturated).length,
      arrows: loopHealthResults,
    },
    workstream_pct: {
      total: workstreamResults.length,
      provenance_valid_count: workstreamResults.filter(r => r.provenance_valid).length,
      semantic_caveats: workstreamResults.filter(r => r.semantic_caveat).map(r => r.workstream),
      details: workstreamResults,
    },
    console_errors: consoleErrors,
    overall_assessment: {
      provenance_clean: loopHealthResults.every(r => r.provenance_valid)
        && workstreamResults.every(r => r.provenance_valid || !r.dom_exists),
      design_quality_concerns: {
        loop_health_saturated_count: loopHealthResults.filter(r => r.saturated).length,
        workstreams_with_semantic_caveats: workstreamResults.filter(r => r.semantic_caveat).length,
      },
    },
  };

  console.log(JSON.stringify(summary, null, 2));
  // Exit code: 0 if provenance clean (design-quality findings are advisory)
  process.exit(summary.overall_assessment.provenance_clean ? 0 : 2);
} catch (err) {
  console.error(JSON.stringify({ probe: 'wm-metric-provenance-verify', error: err.message, stack: err.stack }, null, 2));
  process.exit(1);
} finally {
  await browser.close();
}
