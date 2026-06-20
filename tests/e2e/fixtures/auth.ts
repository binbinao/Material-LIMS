/**
 * Auth fixture — yields a Playwright test with an already-logged-in page.
 *
 * Uses the local-dev DevAuthFilter path (username/password). The OAuth2
 * Azure AD path is exercised by the backend test suite (see
 * lims-service/src/test/java/.../AuthServiceIdTokenVerificationTest.java).
 *
 * Usage:
 *   import { test, expect } from './fixtures/auth'
 *   test('my flow', async ({ page, loginPage }) => { ... })
 */
import { test as base, Page } from '@playwright/test'
import { LoginPage } from '../pages/LoginPage'

export type AuthFixtures = {
  loginPage: LoginPage
  loggedInPage: Page
}

export const test = base.extend<AuthFixtures>({
  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page))
  },

  loggedInPage: async ({ page }, use) => {
    const user = process.env.E2E_USER ?? 'admin'
    const password = process.env.E2E_PASSWORD ?? 'admin123'

    const loginPage = new LoginPage(page)
    await loginPage.goto()
    await loginPage.login(user, password)
    await loginPage.expectLoggedIn()
    await use(page)
  },
})

export { expect } from '@playwright/test'
