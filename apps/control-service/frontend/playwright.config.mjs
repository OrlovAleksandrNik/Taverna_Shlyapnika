import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests",
  timeout: 30_000,
  expect: {
    timeout: 5_000
  },
  use: {
    baseURL: "http://localhost:4191",
    trace: "on-first-retry"
  },
  webServer: {
    command: "node scripts/static-server.mjs",
    url: "http://localhost:4191",
    reuseExistingServer: true,
    timeout: 15_000
  },
  projects: [
    {
      name: "desktop",
      use: { ...devices["Desktop Chrome"] }
    },
    {
      name: "mobile",
      use: { ...devices["Pixel 7"] }
    }
  ]
});
