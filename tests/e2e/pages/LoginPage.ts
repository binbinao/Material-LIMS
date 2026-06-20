/**
 * LoginPage — POM for the Azure AD / local-dev login screen.
 *
 * Material-LIMS supports two auth paths:
 *   1. Azure AD OAuth2 (the production flow — handled by AuthService.java)
 *   2. Local-dev fallback (DevAuthFilter — username + password, no Azure round-trip)
 *
 * The smoke flow uses path 2 because path 1 requires a live Azure tenant and
 * is not reproducible in CI. The OAuth2 happy-path is covered separately by
 * AuthServiceIdTokenVerificationTest.java (backend, source-level).
 */
import { Page, Locator, expect } from '@playwright/test'

export class LoginPage {
  readonly page: Page
  readonly usernameInput: Locator
  readonly passwordInput: Locator
  readonly submitButton: Locator
  readonly errorBanner: Locator

  constructor(page: Page) {
    this.page = page
    // Ant Design Form inputs render as `<input>` wrapped in `<label>`. Select by
    // accessible name; Ant binds the label via `for=`.
    this.usernameInput = page.getByLabel(/username|用户名|账号/i).first()
    this.passwordInput = page.getByLabel(/password|密码/i).first()
    this.submitButton = page.getByRole('button', { name: /sign in|登录|login/i }).first()
    this.errorBanner = page.locator('[data-testid="login-error"], .ant-message-error').first()
  }

  async goto() {
    await this.page.goto('/user/login')
    await this.page.waitForLoadState('networkidle')
  }

  async login(username: string, password: string) {
    await this.usernameInput.fill(username)
    await this.passwordInput.fill(password)
    await this.submitButton.click()
  }

  async expectLoggedIn() {
    await expect(this.page).toHaveURL(/\/($|workspace|dashboard)/i, { timeout: 15_000 })
  }

  async expectLoginError(message?: RegExp) {
    await expect(this.errorBanner).toBeVisible()
    if (message) {
      await expect(this.errorBanner).toContainText(message)
    }
  }
}
