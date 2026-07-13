import { test, expect } from "@playwright/test";

// Smoke tests hit real backend on :18080 via the dev/preview frontend. They are
// deliberately loose — any h1/nav that renders is a pass, because seeded data
// varies between local dev and CI.

test("homepage_loads", async ({ page }) => {
  await page.goto("/");
  await expect(page.locator("h1")).toBeVisible();

  // Nav link should route to /politicians
  await page.getByRole("link", { name: /mps/i }).first().click();
  await expect(page).toHaveURL(/\/politicians$/);
  await expect(page.locator("h1")).toBeVisible();
});

test("politician_search", async ({ page }) => {
  await page.goto("/politicians");
  await expect(page.locator("h1")).toBeVisible();

  const search = page.getByRole("searchbox", { name: /search mps by name/i });
  await search.fill("aab");

  // Wait for either a card containing "Aab" or an empty state — both prove the
  // search wired up and query fired.
  await expect(
    page.locator("body").filter({ hasText: /Aab|Showing 0/i }),
  ).toBeVisible();
});

test("politician_profile", async ({ page }) => {
  await page.goto("/politicians/jaak-aab");

  // Wait until either the profile heading or a not-found/error message renders.
  await page.waitForLoadState("networkidle");
  const heading = page.locator("h1");
  await expect(heading).toBeVisible();

  // Best-effort: the metrics section renders when the profile actually loaded.
  // Don't fail if data is missing — the smoke test just proves the route mounts.
  const metricsSection = page.locator('section[aria-label="Metrics"]');
  if (await metricsSection.count()) {
    await expect(metricsSection).toBeVisible();
  }
});

test("compare_flow", async ({ page }) => {
  await page.goto("/compare");
  await expect(page.locator("h1")).toBeVisible();

  // Each MpPicker wraps its search box + listbox in a flex-1 min-w-[240px] div.
  // Scope autocomplete lookups to the parent so the two pickers stay independent.
  const leftPicker = page.getByRole("searchbox", { name: /left mp/i }).locator("..");
  const rightPicker = page.getByRole("searchbox", { name: /right mp/i }).locator("..");

  await leftPicker.getByRole("searchbox").fill("aab");
  const leftOption = leftPicker.locator("ul button").first();
  await leftOption.waitFor({ state: "visible", timeout: 10_000 });
  await leftOption.click();

  await rightPicker.getByRole("searchbox").fill("akke");
  const rightOption = rightPicker.locator("ul button").first();
  await rightOption.waitFor({ state: "visible", timeout: 10_000 });
  await rightOption.click();

  // Once both MPs are picked, the compare card should render either an
  // agreement section or a loading spinner while it computes.
  await expect(
    page.locator('section[aria-label="Agreement"], [role="status"]'),
  ).toBeVisible({ timeout: 15_000 });
});
