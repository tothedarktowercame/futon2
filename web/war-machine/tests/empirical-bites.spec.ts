import { expect, test } from "@playwright/test";

// These tests prove the session→spine→bite upsample is plumbed through:
//   - bite-edges <g.bite-edges> exist in aif-stack mode
//   - the next-move tile reports an "Empirical pressure" line with N/M
//   - clicking C1 shows per-bite hit annotations in the detail box
test.describe("empirical bite upsample (session evidence → spine → conflict)", () => {
  test("aif-stack view renders bite edges with empirical/logical split", async ({
    page,
  }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(1500);

    // Switch to aif-stack
    await page.locator('[data-testid="view-toggle"]').click();
    await expect(page.locator('[data-testid="view-toggle"]')).toHaveText(
      /aif-stack/,
    );
    await page.waitForTimeout(500);

    // bite-edges group should exist with at least one line.  SVG <line>
    // elements don't satisfy Playwright's visibility heuristic, so check
    // attachment + count instead.
    const lines = page.locator("g.bite-edges line");
    await expect(lines.first()).toBeAttached({ timeout: 5_000 });
    expect(await lines.count()).toBeGreaterThan(0);
  });

  test("next-move tile reports empirical bite coverage", async ({ page }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="next-move"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(1500);

    const text = await page.locator('[data-testid="next-move"]').innerText();
    // Format: "Empirical pressure: C1 bites N/M spine targets in window"
    expect(text).toMatch(/Empirical pressure:\s+\S+\s+bites\s+\d+\/\d+/);
  });

  test("conflict detail enumerates per-bite hit counts", async ({ page }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(1500);

    await page.locator('[data-testid="next-move-jump"]').click();
    const detail = page.locator('[data-testid="detail"]');

    // Detail should list each bite on its own line — either with a hit count
    // or with the "no empirical hit yet" annotation.  Both are honest.
    await expect(detail).toContainText(
      /S\d.*(hit in window|no empirical hit yet)/,
    );
    await expect(detail).toContainText(
      /Empirical bite coverage:\s+\d+\/\d+/,
    );
  });
});
