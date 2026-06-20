/**
 * Playwright E2E config for Material-LIMS.
 *
 * Drives the React frontend against a running backend (locally: docker-compose up,
 * CI: services started by the workflow). Tests run from the project root so both
 * `lims-web-ui` (built artifacts) and `lims-web` (running on :8080) are reachable.
 *
 * Run:
 *   npx playwright test                # headless, parallel
 *   npx playwright test --ui           # interactive mode
 *   npx playwright test --headed       # watch the browser
 *   npx playwright codegen             # record new flows
 */
import { defineConfig, devices } from '@playwright/test'

const BASE_URL = process.env.BASE_URL ?? 'http://localhost:8000'
const API_URL = process.env.API_URL ?? 'http://localhost:8080/api/v1'

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['junit', { outputFile: 'playwright-results.xml' }],
  ],
  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10_000,
    navigationTimeout: 30_000,
    extraHTTPHeaders: {
      'X-Client': 'playwright-e2e',
    },
  },
  expect: {
    timeout: 5_000,
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],
  webServer: process.env.CI
    ? undefined
    : {
        command: 'cd lims-web-ui && npm run dev',
        url: BASE_URL,
        reuseExistingServer: true,
        timeout: 120_000,
      },
  metadata: {
    apiBaseURL: API_URL,
  },
})
