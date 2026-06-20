/**
 * Login smoke — verifies that the local-dev DevAuthFilter path works.
 *
 * This covers the front-end redirect, form submission, and post-login
 * navigation. It does NOT cover Azure AD OAuth2 (that requires a live
 * tenant and is exercised by AuthServiceIdTokenVerificationTest on the
 * backend).
 */
import { test, expect } from '../fixtures/auth'

test.describe('Login (local-dev DevAuthFilter)', () => {
  test('happy path: admin logs in and lands on workspace', async ({ loggedInPage }) => {
    await expect(loggedInPage).toHaveURL(/\/($|workspace|dashboard)/i)
    // Workspace greets the user by username (Ant ProLayout avatar dropdown).
    await expect(loggedInPage.getByText(/admin/i).first()).toBeVisible()
  })

  test('rejects bad password', async ({ page, loginPage }) => {
    await loginPage.goto()
    await loginPage.login('admin', 'definitely-wrong-password')
    await loginPage.expectLoginError()
    // The URL should still be the login page (no redirect).
    await expect(page).toHaveURL(/\/user\/login/)
  })

  test('redirects unauthenticated requests to /user/login', async ({ page }) => {
    await page.goto('/request/list')
    await expect(page).toHaveURL(/\/user\/login\?.*redirect/i, { timeout: 10_000 })
  })
})
