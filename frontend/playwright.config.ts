import { defineConfig, devices } from "@playwright/test";

// Smoke tests target the running dev stack. Locally this expects:
//   - frontend on http://localhost:5173 (npm run dev  OR  docker-compose web on :5173)
//   - backend  on http://localhost:18080 (docker-compose api)
// Override via PLAYWRIGHT_BASE_URL for CI (e.g. `npm run preview` on a different port).
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: 2,
  timeout: 30_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? [["list"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? "http://localhost:5173",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
