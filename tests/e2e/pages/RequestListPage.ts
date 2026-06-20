/**
 * RequestListPage — POM for the request list / kanban landing page.
 *
 * The frontend exposes this at `/request/list` (table view) and `/request/kanban`
 * (board view). The table is the canonical flow used by the lifecycle smoke test.
 */
import { Page, Locator, expect } from '@playwright/test'

export class RequestListPage {
  readonly page: Page
  readonly newRequestButton: Locator
  readonly searchInput: Locator
  readonly statusFilter: Locator
  readonly tableRows: Locator

  constructor(page: Page) {
    this.page = page
    this.newRequestButton = page.getByRole('button', { name: /new request|新建申请/i }).first()
    this.searchInput = page.getByPlaceholder(/search|搜索/i).first()
    this.statusFilter = page.locator('.ant-select').filter({ hasText: /status|状态/i }).first()
    this.tableRows = page.locator('.ant-table-tbody > tr')
  }

  async goto() {
    await this.page.goto('/request/list')
    await this.page.waitForLoadState('networkidle')
  }

  async openNewRequestForm() {
    await this.newRequestButton.click()
    await this.page.waitForURL(/\/request\/create|\/request\/new/i, { timeout: 10_000 })
  }

  async search(keyword: string) {
    await this.searchInput.fill(keyword)
    await this.page.waitForResponse(
      (resp) => resp.url().includes('/api/v1/requests') && resp.status() === 200,
      { timeout: 10_000 }
    )
    await this.page.waitForLoadState('networkidle')
  }

  async filterByStatus(status: 'DRAFT' | 'SUBMITTED' | 'ASSIGNED' | 'COMPLETED') {
    await this.statusFilter.click()
    await this.page.locator('.ant-select-item-option').filter({ hasText: new RegExp(status, 'i') }).click()
    await this.page.waitForLoadState('networkidle')
  }

  async expectRowCount(min: number) {
    const count = await this.tableRows.count()
    expect(count).toBeGreaterThanOrEqual(min)
  }

  async openFirstRequestWithStatus(status: string) {
    const row = this.tableRows.filter({ hasText: new RegExp(status, 'i') }).first()
    await row.locator('a, button').first().click()
    await this.page.waitForURL(/\/request\/detail/i, { timeout: 10_000 })
  }
}
