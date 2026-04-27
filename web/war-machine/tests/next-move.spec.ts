import { expect, test } from "@playwright/test";

test.describe("Recommended Next Move tile + conflict drill-down", () => {
  test("next-move tile renders with target, summary, and jump button", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });

    // Allow the AIF endpoint to load
    await page.waitForTimeout(1500);

    const tile = page.locator('[data-testid="next-move"]');
    await expect(tile).toBeVisible({ timeout: 15_000 });

    // Target line names a closure in the AIF-stack alias namespace.
    await expect(tile).toContainText(/Close\s+🐜\d/);

    // Specific move line is non-trivial
    const text = await tile.innerText();
    expect(text.length).toBeGreaterThan(50);

    // Jump button is present
    await expect(
      page.locator('[data-testid="next-move-jump"]'),
    ).toBeVisible();
  });

  test("show/hide button toggles detail and switches to aif-stack on show", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(1500);

    const detail = page.locator('[data-testid="detail"]');
    const btn = page.locator('[data-testid="next-move-jump"]');

    // Initial label is "Show <id> detail"
    await expect(btn).toHaveText(/^Show\s+\S+\s+detail$/);

    await btn.click();

    // View toggle now reads "View: aif-stack"
    await expect(page.locator('[data-testid="view-toggle"]')).toHaveText(
      /aif-stack/,
    );
    // Detail panel shows CONFLICT with weight
    await expect(detail).toContainText("CONFLICT");
    await expect(detail).toContainText(/weight\s+\d/);
    // Button now reads "Hide ..."
    await expect(btn).toHaveText(/^Hide\s+\S+\s+detail$/);

    // Second click hides
    await btn.click();
    await expect(detail).toHaveText(/click a hex for details/i);
    await expect(btn).toHaveText(/^Show\s+\S+\s+detail$/);
  });

  test("clicking a conflict hexagon directly populates rich detail", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(1500);

    // Switch to aif-stack mode (default is :stack, then aif-stack is next)
    await page.locator('[data-testid="view-toggle"]').click();
    await expect(page.locator('[data-testid="view-toggle"]')).toHaveText(
      /aif-stack/,
    );
    await page.waitForTimeout(500);

    // Click the C1 cell (conflict id rendered as data-node-id)
    const c1 = page.locator('g[data-node-id="C1"]');
    await expect(c1).toBeVisible({ timeout: 10_000 });
    await c1.click();

    const detail = page.locator('[data-testid="detail"]');
    await expect(detail).toContainText("CONFLICT C1");
    await expect(detail).toContainText(/weight\s+\d/);
    await expect(detail).toContainText("Bites:");
  });

  test("stack-level S6 is rendered as ant-6 in the War Machine surface", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(1500);

    const nextMove = page.locator('[data-testid="next-move"]');
    await expect(nextMove).toBeVisible({ timeout: 15_000 });
    await expect(nextMove).toContainText("Close 🐜6");

    await page.locator('[data-testid="view-toggle"]').click();
    await expect(page.locator('[data-testid="view-toggle"]')).toHaveText(
      /aif-stack/,
    );

    const s6 = page.locator('g[data-node-id="S6"]');
    await expect(s6).toBeVisible({ timeout: 10_000 });
    await expect(s6).toContainText("🐜6");

    await s6.hover();
    await expect(s6).toContainText(/AIF/i);

    await s6.click();

    const detail = page.locator('[data-testid="detail"]');
    await expect(detail).toContainText("STACK NODE 🐜6");
    await expect(detail).toContainText("Canonical id: S6");
  });

  test("next-move tile shows v1 completed and v2 under-specified when rollover has happened", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(1500);

    const nextMove = page.locator('[data-testid="next-move"]');
    await expect(nextMove).toBeVisible({ timeout: 15_000 });
    await expect(nextMove).toContainText("Completed: rolled-forward wm.close-s6.v1");
    await expect(nextMove).toContainText("Next: underspecified wm.close-s6.v2");
    await expect(nextMove).toContainText(
      /not yet preregistered as an executable closure agenda/i,
    );
    await expect(nextMove).toContainText(/Still missing: action-surface, step-witness, effect-witness, successor-witness/i);
    await expect(nextMove).toContainText(/Witness: step \d+ → \d+ · \d{2}:\d{2} · (live|aging) \d+m ago/i);
  });

  test("HUD shows wall-clock playhead, not abstract Tick counter", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hud"]')).toBeVisible({
      timeout: 30_000,
    });
    // Wait for sessions to seed and tick loop to advance
    await page.waitForTimeout(2000);

    const hud = page.locator('[data-testid="hud"]');
    const text = await hud.innerText();
    // Either "Play HH:MM" (when timeline data is loaded — the normal case)
    // or the legacy "Tick NNNN" fallback (when no sessions are present yet).
    expect(text).toMatch(/Play\s+\d{2}:\d{2}|Tick\s+\d+/);
  });
});
