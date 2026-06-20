/**
 * RequestDetailPage — POM for the request detail / workflow view.
 *
 * Drives the request lifecycle: DRAFT → SUBMITTED → ASSIGNED → SAMPLING → REPORTING.
 * Each transition is a button click + Ant Design Modal confirm + wait for the
 * underlying PUT/POST to /api/v1/requests/{id}/{action}.
 */
import { Page, Locator, expect } from '@playwright/test'

export class RequestDetailPage {
  readonly page: Page
  readonly statusTag: Locator
  readonly submitButton: Locator
  readonly assignButton: Locator
  readonly rejectButton: Locator
  readonly receiveSampleButton: Locator
  readonly startReportingButton: Locator
  readonly completeButton: Locator

  constructor(page: Page) {
    this.page = page
    this.statusTag = page.locator('.ant-tag').first()
    this.submitButton = page.getByRole('button', { name: /submit|提交/i }).first()
    this.assignButton = page.getByRole('button', { name: /assign|分派|分配/i }).first()
    this.rejectButton = page.getByRole('button', { name: /reject|驳回/i }).first()
    this.receiveSampleButton = page.getByRole('button', { name: /receive sample|收样/i }).first()
    this.startReportingButton = page.getByRole('button', { name: /start report|开始报告/i }).first()
    this.completeButton = page.getByRole('button', { name: /complete|完成/i }).first()
  }

  async goto(requestId: string) {
    await this.page.goto(`/request/detail/${requestId}`)
    await this.page.waitForLoadState('networkidle')
  }

  async expectStatus(status: string) {
    await expect(this.statusTag).toContainText(new RegExp(status, 'i'), { timeout: 10_000 })
  }

  async submit() {
    await this.submitButton.click()
    await this.page.waitForResponse(
      (resp) => resp.url().includes('/submit') && resp.request().method() === 'POST',
      { timeout: 10_000 }
    )
    await this.page.waitForLoadState('networkidle')
  }

  async assignTo(engineerUsername: string) {
    await this.assignButton.click()
    const modal = this.page.locator('.ant-modal').filter({ hasText: /assign/i })
    await modal.locator('.ant-select').first().click()
    await this.page.locator('.ant-select-item-option').filter({ hasText: engineerUsername }).click()
    await modal.getByRole('button', { name: /ok|确定/i }).click()
    await this.page.waitForResponse(
      (resp) => resp.url().includes('/assign') && resp.request().method() === 'POST',
      { timeout: 10_000 }
    )
    await this.page.waitForLoadState('networkidle')
  }
}
