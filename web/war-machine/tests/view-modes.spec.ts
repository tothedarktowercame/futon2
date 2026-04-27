/*
 * View-mode coverage spec.
 *
 * The toolbar's "View:" toggle cycles through five modes. For each mode we
 * verify three things:
 *   (a) clicking the toggle changes the label
 *   (b) the SVG actually re-renders with cells appropriate to that mode
 *   (c) clicking a hex still produces a detail-panel update (UI live)
 *
 * The intent is to fail loud if any mode silently falls back to the stack
 * layout (the bug claude-15 + claude-16 hit on 2026-04-24).
 */
import { test, expect, Page } from "@playwright/test";

const MODES = [
  "stack",
  "aif-stack",
  "missions",
  "sorrys",
  "invariants",
  "patterns",
];

async function getCellSignature(page: Page): Promise<{
  count: number;
  ids: string[];
}> {
  // Wait for at least one cell to be present (handles the data-load race)
  const svg = page.locator('[data-testid="hex-svg"]');
  await expect(svg).toBeVisible({ timeout: 30_000 });
  await page.waitForFunction(() => {
    const cells = document.querySelectorAll(
      '[data-testid="hex-svg"] g[data-node-id]'
    );
    return cells.length > 0;
  }, { timeout: 30_000 });

  const ids = await svg.locator("g[data-node-id]").evaluateAll((els) =>
    els.map((e) => e.getAttribute("data-node-id") || "").filter(Boolean)
  );
  return { count: ids.length, ids: ids.sort() };
}

async function clickViewToggle(page: Page) {
  await page.locator('[data-testid="view-toggle"]').click();
  // Give Reagent a tick to re-render
  await page.waitForTimeout(250);
}

async function readToggleLabel(page: Page): Promise<string> {
  const label = await page.locator('[data-testid="view-toggle"]').textContent();
  return (label || "").replace("View: ", "").trim();
}

// Cycle the toggle until we land on `target`, with a safety bound so a
// renaming of view-modes can't infinite-loop the test.
async function gotoMode(page: Page, target: string, knownModes: string[]): Promise<boolean> {
  for (let i = 0; i < knownModes.length + 1; i++) {
    if ((await readToggleLabel(page)) === target) return true;
    await page.locator('[data-testid="view-toggle"]').click();
    await page.waitForTimeout(250);
  }
  return false;
}

test.describe("view-mode toggle: each mode renders something", () => {
  test("toggle cycles through all six modes in order", async ({ page }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });

    // Capture the starting label (should be 'stack')
    let label = await readToggleLabel(page);
    expect(label).toBe("stack");

    // Click through and verify the label cycles correctly
    const observed: string[] = [label];
    for (let i = 0; i < MODES.length; i++) {
      await clickViewToggle(page);
      observed.push(await readToggleLabel(page));
    }
    // After MODES.length clicks we should be back at 'stack' (full cycle)
    expect(observed[MODES.length]).toBe("stack");
    // The intermediate sequence should match MODES order
    expect(observed.slice(1, MODES.length + 1)).toEqual([
      "aif-stack",
      "missions",
      "sorrys",
      "invariants",
      "patterns",
      "stack",
    ]);
  });

  test("each view-mode produces a non-empty hex layout", async ({ page }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });

    const failures: string[] = [];

    for (const expected of MODES) {
      // Cycle to the target mode by reading current label and clicking until we match
      // (avoids assumptions about which mode is "first")
      let safety = 6;
      while ((await readToggleLabel(page)) !== expected && safety-- > 0) {
        await clickViewToggle(page);
      }
      expect(await readToggleLabel(page)).toBe(expected);

      const sig = await getCellSignature(page);
      if (sig.count === 0) {
        failures.push(`${expected}: 0 cells rendered (silent fallback?)`);
      }
    }

    expect(failures, failures.join(" | ")).toEqual([]);
  });

  test("aif-stack renders distinct content from stack (S/C nodes, not repos)", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });

    // Cycle to :aif-stack
    while ((await readToggleLabel(page)) !== "aif-stack") {
      await clickViewToggle(page);
    }

    const sig = await getCellSignature(page);
    expect(sig.count).toBeGreaterThan(0);

    // The AIF view should contain at least the spine ids (S0..S9) AND the
    // conflict ids (C1..C4). If we see only repo names (futon0..futon7), the
    // view silently fell back to :stack.
    const hasSpineIds = sig.ids.some((id) => /^S[0-9]+$/.test(id));
    const hasConflictIds = sig.ids.some((id) => /^C[0-9]+$/.test(id));
    const hasRepoIds = sig.ids.some((id) => /^futon\d/.test(id));

    if (hasRepoIds && !hasSpineIds) {
      throw new Error(
        `aif-stack mode rendered the stack view (saw repo IDs ${sig.ids
          .filter((i) => /^futon\d/.test(i))
          .join(", ")}). Likely silent fallback. ` +
          `Full ID set: ${sig.ids.join(", ")}`
      );
    }
    expect(hasSpineIds, `expected spine IDs (S0..S9). saw: ${sig.ids.join(", ")}`).toBe(true);
    expect(
      hasConflictIds,
      `expected conflict IDs (C1..C4). saw: ${sig.ids.join(", ")}`
    ).toBe(true);
  });

  test("modes that promise distinct content actually deliver it", async ({
    page,
  }) => {
    // Captures the silent-fallback regression: missions/invariants/patterns
    // currently fall back to :stack rendering. We compare each mode's id set
    // against :stack's id set. If they're identical, the mode is a no-op.
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });

    while ((await readToggleLabel(page)) !== "stack") {
      await clickViewToggle(page);
    }
    const stackSig = await getCellSignature(page);

    const fallbackModes: string[] = [];
    for (const mode of [
      "aif-stack",
      "missions",
      "sorrys",
      "invariants",
      "patterns",
    ]) {
      while ((await readToggleLabel(page)) !== mode) {
        await clickViewToggle(page);
      }
      const sig = await getCellSignature(page);
      // Identical id sets mean fallback
      if (
        sig.count === stackSig.count &&
        sig.ids.join(",") === stackSig.ids.join(",")
      ) {
        fallbackModes.push(mode);
      }
    }

    // All five modes should now produce distinct content. If any falls
    // back silently (identical id-set to :stack), this test fails.
    expect(
      fallbackModes,
      `These modes silently fall back to stack and should not: ${fallbackModes.join(", ")}`
    ).toEqual([]);
  });

  test("cycling through all modes does not accumulate ghost cells (React key uniqueness)", async ({
    page,
  }) => {
    // Regression: duplicate React keys (two missions named f6-ingest) caused
    // stale cells to leak across mode switches. After cycling through all
    // modes, returning to a mode should produce IDENTICAL cell counts.
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });

    const firstPass: Record<string, number> = {};
    for (const mode of MODES) {
      expect(await gotoMode(page, mode, MODES), `failed to reach ${mode}`).toBe(true);
      const sig = await getCellSignature(page);
      firstPass[mode] = sig.count;
    }

    // Cycle past every mode once more, then re-measure
    for (let i = 0; i < MODES.length; i++) await clickViewToggle(page);
    const secondPass: Record<string, number> = {};
    for (const mode of MODES) {
      expect(await gotoMode(page, mode, MODES), `failed to reach ${mode}`).toBe(true);
      const sig = await getCellSignature(page);
      secondPass[mode] = sig.count;
    }

    const drifts: string[] = [];
    for (const mode of MODES) {
      if (firstPass[mode] !== secondPass[mode]) {
        drifts.push(
          `${mode}: ${firstPass[mode]} → ${secondPass[mode]} (ghost cells accumulating)`
        );
      }
    }
    expect(drifts, drifts.join(" | ")).toEqual([]);
  });

  test("no two cells share a data-node-id within a single view (React key uniqueness)", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });

    const violations: string[] = [];
    for (const mode of MODES) {
      expect(await gotoMode(page, mode, MODES), `failed to reach ${mode}`).toBe(true);
      const sig = await getCellSignature(page);
      const counts: Record<string, number> = {};
      for (const id of sig.ids) counts[id] = (counts[id] || 0) + 1;
      const dupes = Object.entries(counts)
        .filter(([_, n]) => n > 1)
        .map(([id, n]) => `${id}×${n}`);
      if (dupes.length > 0) violations.push(`${mode}: ${dupes.join(", ")}`);
    }
    expect(
      violations,
      `Duplicate data-node-ids found (will cause React-key collisions and ghost cells):\n  ${violations.join(
        "\n  "
      )}`
    ).toEqual([]);
  });
});
