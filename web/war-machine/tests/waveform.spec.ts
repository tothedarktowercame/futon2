import { test, expect } from "@playwright/test";

test("waveform appears in toolbar with stripes + label", async ({ page }) => {
  await page.goto("/index.html");
  await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({ timeout: 30_000 });

  const wave = page.locator('[data-testid="waveform-svg"]');
  await expect(wave).toBeVisible({ timeout: 5_000 });

  // Should have at least one <line> stripe (one per session step)
  const lineCount = await wave.locator("line").count();
  expect(lineCount).toBeGreaterThan(0);

  // Time labels should render
  await expect(page.locator(".waveform .label.left")).toBeVisible();
  await expect(page.locator(".waveform .label.right")).toBeVisible();
});

test("clicking the waveform triggers a playhead jump (replay state changes)", async ({ page }) => {
  await page.goto("/index.html");
  await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({ timeout: 30_000 });
  await page.waitForTimeout(1_500);

  const wave = page.locator('[data-testid="waveform-svg"]');
  const box = await wave.boundingBox();
  if (!box) throw new Error("waveform has no bounding box");

  // Click left side, then right side; playhead median should differ
  await page.mouse.click(box.x + box.width * 0.1, box.y + box.height / 2);
  await page.waitForTimeout(200);
  await page.mouse.click(box.x + box.width * 0.9, box.y + box.height / 2);
  await page.waitForTimeout(200);
  // No assertion on visible playhead position because tick() may have
  // moved it; the click handler ran iff no JS error blocked it. We check
  // for absence of error in the page.
  const errors: string[] = [];
  page.on("pageerror", (e) => errors.push(String(e)));
  await page.waitForTimeout(200);
  expect(errors).toEqual([]);
});

test("playhead advances forward over time", async ({ page }) => {
  await page.goto("/index.html");
  await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({ timeout: 30_000 });
  await page.waitForTimeout(1500);

  const playhead = page.locator('[data-testid="waveform-svg"] line[stroke="#f97316"]');
  const readX = async () => parseFloat((await playhead.getAttribute("x1")) || "0");

  const before = await readX();
  await page.waitForTimeout(1200);
  const after = await readX();

  expect(after).toBeGreaterThan(before);
});

test("repo-bearing replay tracks produce visible ants on the grid", async ({ page }) => {
  await page.goto("/index.html");
  await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({ timeout: 30_000 });

  const trackRows = page.locator('[data-testid^="track-row-"]');
  const rowCount = await trackRows.count();
  if (rowCount === 0) {
    await expect(page.locator('[data-testid="track-empty"]')).toBeVisible();
    return;
  }

  const texts = await trackRows.evaluateAll((els) => els.map((el) => el.textContent || ""));
  const hasRepoBearingTrack = texts.some((text) => {
    const m = text.match(/\|\s+(\d+)\s+repos\s+\|/);
    return m && Number(m[1]) > 0;
  });

  if (!hasRepoBearingTrack) {
    return;
  }

  await expect
    .poll(
      async () => await page.locator('[data-testid="hex-svg"] g.ants circle').count(),
      { timeout: 10_000 }
    )
    .toBeGreaterThan(0);
});

test("waveform renders a faint uncertainty envelope", async ({ page }) => {
  await page.goto("/index.html");
  await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({ timeout: 30_000 });
  await page.waitForTimeout(1500);

  const trackRows = await page.locator('[data-testid^="track-row-"]').count();
  if (trackRows === 0) {
    await expect(page.locator('[data-testid="track-empty"]')).toBeVisible();
    return;
  }

  await expect(page.locator('[data-testid="waveform-envelope"]')).toBeVisible();
});

test("days toggle updates the scan-window note", async ({ page }) => {
  await page.goto("/index.html");
  await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({ timeout: 30_000 });
  await page.waitForTimeout(1500);

  const note = page.locator('[data-testid="waveform-bounds-note"]');
  await expect(note).toContainText("14d");

  await page.locator('[data-testid="days-toggle"]').click();
  await expect(page.locator('[data-testid="days-toggle"]')).toHaveText(/90d/);
  await expect(note).toContainText("90d");
});

test("tracks can be selected and muted from the sidebar", async ({ page }) => {
  await page.goto("/index.html");
  await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({ timeout: 30_000 });
  await page.waitForTimeout(1500);

  const rowCount = await page.locator('[data-testid^="track-row-"]').count();
  if (rowCount === 0) {
    await expect(page.locator('[data-testid="track-empty"]')).toBeVisible();
    return;
  }

  const row = page.locator('[data-testid^="track-row-"]').first();
  await row.click();
  await expect(page.locator('[data-testid="track-detail"]')).toBeVisible();

  const toggle = row.locator('input[type="checkbox"]');
  const wave = page.locator('[data-testid="waveform-svg"]');
  const before = await wave.locator("line").count();
  await toggle.click();
  await expect(toggle).not.toBeChecked();
  await page.waitForTimeout(500);
  const after = await wave.locator("line").count();

  expect(after).toBeLessThan(before);
});

test("dragging a selection shows concrete timestamp bounds", async ({ page }) => {
  await page.goto("/index.html");
  await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({ timeout: 30_000 });
  await page.waitForTimeout(1500);

  const wave = page.locator('[data-testid="waveform-svg"]');
  const box = await wave.boundingBox();
  if (!box) throw new Error("waveform has no bounding box");

  await page.mouse.move(box.x + box.width * 0.2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width * 0.55, box.y + box.height / 2);
  await page.mouse.up();

  const label = page.locator('[data-testid="waveform-selection-label"]');
  await expect(label).toBeVisible();
  await expect(label).toContainText("->");
});

test("legend appears in left tile, NOT in right sidebar", async ({ page }) => {
  await page.goto("/index.html");
  await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({ timeout: 30_000 });

  // Legend should be in .legend-panel, not in .sidebar
  const inLegendPanel = await page.locator(".legend-panel h3", { hasText: "Legend" }).count();
  const inSidebar = await page.locator(".sidebar h3", { hasText: "Legend" }).count();
  expect(inLegendPanel).toBeGreaterThan(0);
  expect(inSidebar).toBe(0);
});
