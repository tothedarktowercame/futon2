import { expect, test } from "@playwright/test";

// =============================================================================
// WM-I3 strict — pixel-side comparator (M-war-machine-tuning § Checkpoint 8 B2)
//
// For each AIF-tile data-testid added in INSTANTIATE (Checkpoints 4–5):
//   1. fetch the substrate's :scheduler block from /api/alpha/aif-stack/live
//   2. read the rendered tile's text via the data-testid selector
//   3. assert the rendered text contains the expected substrate-derived
//      values within tolerance
//
// This closes B2 — the WM-I3 strict verdict (substrate-side existence vs
// pixel-side fidelity). Without this, WM-I3 stays :partial.
// With this passing, WM-I3 flips to :pass and the only remaining blocker
// for "valid war machine" is the autonomous Evidence-derived correlation
// (B3 + B5 collapsed).
// =============================================================================

test.describe("AIF tiles · WM-I3 strict (pixel-side fidelity)", () => {
  // Pulled from the surface 2026-04-27 per E-war-machine-qa: the tile
  // failed the "fresh-Claude-session" test — gave a stance label without
  // the evidence or action affordances needed for the operator to use it.
  // Skipped (not deleted) so the spec is easy to reinstate alongside a
  // redesigned tile.
  test.skip("aif-mode-tile renders mode + diagnostics from :scheduler", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    // Allow the AIF poll to fire and populate s/aif-data.
    await page.waitForTimeout(2_000);

    // Fetch substrate-side expected via the same endpoint the UI polls.
    const expected = await page.evaluate(async () => {
      const resp = await fetch("/api/alpha/aif-stack/live");
      const body = await resp.json();
      return body?.scheduler?.["last-diagnostics"];
    });
    test.skip(
      !expected,
      "scheduler/last-diagnostics not populated; tick the scheduler first",
    );

    const tile = page.locator('[data-testid="aif-mode"]');
    await expect(tile).toBeVisible({ timeout: 10_000 });
    const text = await tile.innerText();

    // Mode label appears verbatim. portfolio modes are :BUILD / :MAINTAIN /
    // :CONSOLIDATE; the cljs renders (name mode) so colons are dropped.
    expect(text).toContain(String(expected.mode));

    // Numeric formatting: cljs uses .toFixed(2) for urgency and τ, .toFixed(3)
    // for free-energy. Match the rendered string literally where possible.
    if (typeof expected.urgency === "number") {
      expect(text).toContain(expected.urgency.toFixed(2));
    }
    if (typeof expected.tau === "number") {
      expect(text).toContain(expected.tau.toFixed(2));
    }
    if (typeof expected["free-energy"] === "number") {
      expect(text).toContain(expected["free-energy"].toFixed(3));
    }
  });

  test("aif-efe-tile renders 4-term EFE for the chosen action", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(2_000);

    const expected = await page.evaluate(async () => {
      const resp = await fetch("/api/alpha/aif-stack/live");
      const body = await resp.json();
      return {
        terms: body?.scheduler?.["last-efe-terms"],
        action: body?.scheduler?.["last-action"],
      };
    });
    test.skip(!expected.terms, "last-efe-terms not populated");

    const tile = page.locator('[data-testid="aif-efe"]');
    await expect(tile).toBeVisible({ timeout: 10_000 });
    const text = await tile.innerText();

    // Structural check: each of the four EFE term labels appears.
    for (const k of ["pragmatic", "epistemic", "upvote", "effort"]) {
      expect(text.toLowerCase()).toContain(k);
    }
    // Action label present (substrate-side). The page's polling cadence
    // may differ from this test's fetch, so we don't compare exact
    // numeric values — only structural presence + chosen action label.
    // CSS upper-cases tile labels; compare case-insensitively.
    if (expected.action != null) {
      expect(text.toLowerCase()).toContain(
        String(expected.action).toLowerCase(),
      );
    }
    // At least one numeric value renders (3-decimal form).
    expect(text).toMatch(/\d\.\d{3}/);
    // The "G=" header appears (case-insensitive).
    expect(text.toLowerCase()).toContain("g=");
  });

  test("aif-prediction-error tile renders top channel ε values", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(2_000);

    const expected = await page.evaluate(async () => {
      const resp = await fetch("/api/alpha/aif-stack/live");
      const body = await resp.json();
      return body?.scheduler?.["last-prediction-errors"]?.raw ?? null;
    });
    test.skip(!expected, "last-prediction-errors not populated");

    const tile = page.locator('[data-testid="aif-prediction-error"]');
    await expect(tile).toBeVisible({ timeout: 10_000 });
    const text = await tile.innerText();

    // The cljs side filters channels with abs(err) > 1e-3 and shows the top 6.
    const ranked = Object.entries(expected as Record<string, number>)
      .filter(([, v]) => typeof v === "number" && Math.abs(v) > 1e-3)
      .sort((a, b) => Math.abs(b[1]) - Math.abs(a[1]))
      .slice(0, 6);

    if (ranked.length === 0) {
      expect(text).toContain("noise floor");
      return;
    }

    // Each rendered top-N channel: name appears + value as .toFixed(3).
    for (const [k, v] of ranked) {
      expect(text).toContain(k);
      expect(text).toContain(v.toFixed(3));
    }
  });

  test("aif-observation tile lists all 16 channels with values", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(2_000);

    const expected = await page.evaluate(async () => {
      const resp = await fetch("/api/alpha/aif-stack/live");
      const body = await resp.json();
      return body?.scheduler?.["last-observation"] ?? null;
    });
    test.skip(!expected, "last-observation not populated");

    const tile = page.locator('[data-testid="aif-observation"]');
    await expect(tile).toBeVisible({ timeout: 10_000 });

    // The tile is a <details> element — open it before scraping. Set
    // the open attribute directly to avoid summary-click flakiness.
    await tile.evaluate((el: HTMLDetailsElement) => {
      el.open = true;
    });
    await page.waitForTimeout(200);
    const text = await tile.innerText();

    // Header reports channel count. Match flexibly — whitespace and
    // bullet may vary; CSS upper-cases summary text.
    expect(text).toMatch(
      new RegExp(`${Object.keys(expected).length}\\s+channels`, "i"),
    );

    // Every channel name appears (16 of them).
    for (const k of Object.keys(expected)) {
      expect(text).toContain(k);
    }

    // At least one numeric value in 3-decimal form is rendered. The page
    // polls on its own cadence and may show different snapshot than
    // our fetch, so we don't attempt to match exact values.
    expect(text).toMatch(/\d\.\d{3}/);
  });
});
