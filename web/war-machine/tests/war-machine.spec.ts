import { test, expect } from "@playwright/test";

test("war machine renders hex map and live HUD", async ({ page }) => {
  await page.goto("/index.html");

  // Wait for SVG to appear (server scan + first render)
  const svg = page.locator('[data-testid="hex-svg"]');
  await expect(svg).toBeVisible({ timeout: 30_000 });

  // SVG should contain at least one hex polygon
  const polygons = svg.locator("polygon");
  const count = await polygons.count();
  expect(count).toBeGreaterThan(0);

  // HUD should be present and lead with a position indicator: either the
  // wall-clock playhead ("Play HH:MM") when timeline data has loaded, or
  // the legacy "Tick NNNN" fallback when it hasn't yet.
  const hud = page.locator('[data-testid="hud"]');
  await expect(hud).toBeVisible();
  await expect(hud).toContainText(/Play\s+\d{2}:\d{2}|Tick\s+\d+/);

  // The leading indicator advances within ~1.5s (tick loop runs at 400ms).
  const first = await hud.textContent();
  await page.waitForTimeout(1500);
  const second = await hud.textContent();
  expect(second).not.toBe(first);

  // Clicking a hex (any <g data-node-id>) should populate the detail panel.
  const hexCell = svg.locator("g[data-node-id]").first();
  await hexCell.click();
  const detail = page.locator('[data-testid="detail"]');
  await expect(detail).not.toHaveText(/click a hex for details/i, { timeout: 2000 });

  await page.screenshot({ path: "tests/war-machine.png", fullPage: true });
});

test("strategic sorry hex expands label on hover and retains canonical id", async ({
  page,
}) => {
  await page.goto("/index.html");

  const svg = page.locator('[data-testid="hex-svg"]');
  await expect(svg).toBeVisible({ timeout: 30_000 });

  const toggle = page.locator('[data-testid="view-toggle"]');
  await expect(toggle).toBeVisible({ timeout: 30_000 });

  for (let i = 0; i < 6; i++) {
    if ((await toggle.textContent())?.includes("sorrys")) break;
    await toggle.click();
    await page.waitForTimeout(250);
  }
  await expect(toggle).toHaveText(/sorrys/);

  const sorryCell = svg.locator('g[data-node-id="SORRY-market-interface"]');
  const detail = page.locator('[data-testid="detail"]');
  await expect(sorryCell).toBeVisible({ timeout: 10_000 });
  await expect(sorryCell).toContainText("🌐1");
  await expect(sorryCell).not.toContainText("market");

  await sorryCell.hover();
  await expect(sorryCell).toContainText("market");
  await expect(sorryCell).not.toContainText("🌐1-market-interface");

  await detail.hover();
  await expect(sorryCell).toContainText("🌐1");

  await sorryCell.click();

  await expect(detail).toContainText("SORRY: 🌐1-market-interface");
  await expect(detail).toContainText("Canonical id: SORRY-market-interface");
});
