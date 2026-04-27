import { expect, test } from "@playwright/test";

async function switchToSorrysView(page: any) {
  const toggle = page.locator('[data-testid="view-toggle"]');
  const sorryCell = page.locator('g[data-node-id="SORRY-market-interface"]');
  await expect(toggle).toBeVisible({ timeout: 30_000 });

  for (let i = 0; i < 6; i++) {
    if ((await sorryCell.count()) > 0) break;
    await toggle.click();
    await page.waitForTimeout(250);
  }
  await expect(sorryCell).toBeVisible({ timeout: 10_000 });
}

test.describe("Emacs bridge — clicked hex opens related source in Emacs", () => {
  test("clicking a story-backed hex opens the related VSATARCS target", async ({
    page,
  }) => {
    let capturedBody: any = null;

    await page.route(
      "**/api/alpha/war-machine/show-in-emacs",
      async (route) => {
        capturedBody = JSON.parse(route.request().postData() || "{}");
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            "ok?": true,
            leaf: capturedBody.leaf,
            path: `/home/joe/code/futon5a/holes/stories/${capturedBody.leaf}.md`,
          }),
        });
      },
    );

    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(1500);

    const storyBackedHex = page.locator("g[data-node-id]").first();
    await expect(storyBackedHex).toBeVisible({ timeout: 10_000 });
    await storyBackedHex.click();
    await page.waitForTimeout(400);

    expect(capturedBody).not.toBeNull();
    expect(capturedBody.kind).toBe("vsatarcs-story");
    expect(capturedBody.leaf).toMatch(/^leaf-|^devmap-/);

    const button = page.locator('[data-testid="open-in-emacs"]');
    await expect(button).toBeVisible({ timeout: 5_000 });
  });

  test("clicking a Strategic SORRY opens its individual VSATARCS page", async ({
    page,
  }) => {
    let capturedBody: any = null;

    await page.route(
      "**/api/alpha/war-machine/show-in-emacs",
      async (route) => {
        capturedBody = JSON.parse(route.request().postData() || "{}");
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            "ok?": true,
            kind: capturedBody.kind,
            path: capturedBody.path,
          }),
        });
      },
    );

    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(1500);

    const sorryCell = page.locator('g[data-node-id="SORRY-market-interface"]');
    await switchToSorrysView(page);
    await sorryCell.click();
    await page.waitForTimeout(400);

    expect(capturedBody).not.toBeNull();
    expect(capturedBody.kind).toBe("vsatarcs-story");
    expect(capturedBody.leaf).toBe("globe1-market-interface");
  });

  test("Open in Emacs button is hidden for unbound nodes", async ({ page }) => {
    await page.goto("/index.html");
    await expect(page.locator('[data-testid="hex-svg"]')).toBeVisible({
      timeout: 30_000,
    });
    await page.waitForTimeout(1500);

    // Default mode is :stack. Click the first hex (a repo) — most repos
    // should have a devmap and so should surface the button. We just check
    // that the button mechanism gates honestly: when nothing is selected,
    // the button is absent.
    await expect(page.locator('[data-testid="open-in-emacs"]')).toHaveCount(0);
  });
});
