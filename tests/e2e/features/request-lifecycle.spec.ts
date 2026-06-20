/**
 * Request lifecycle smoke — drives a request through the three core
 * workflow transitions: DRAFT → SUBMITTED → ASSIGNED.
 *
 * Why these three:
 *   - DRAFT → SUBMITTED is the requester's first action. It calls
 *     `submitRequest` on the backend, which kicks off the Flowable process.
 *   - SUBMITTED → ASSIGNED is the manager's action. It calls `assignRequest`,
 *     which now (since issue #36) validates that each taskId belongs to the
 *     request before flipping the status.
 *   - ASSIGNED is the steady state from which SAMPLING / REPORTING /
 *     APPROVING / COMPLETED all branch. This smoke only verifies that the
 *     request reaches it.
 */
import { test, expect } from '../fixtures/auth'
import { RequestListPage } from '../pages/RequestListPage'
import { RequestDetailPage } from '../pages/RequestDetailPage'

test.describe('Request lifecycle', () => {
  test('DRAFT → SUBMITTED → ASSIGNED', async ({ loggedInPage }) => {
    const list = new RequestListPage(loggedInPage)
    const detail = new RequestDetailPage(loggedInPage)

    await list.goto()
    await list.expectRowCount(1)
    await list.openFirstRequestWithStatus('DRAFT')

    await expect(loggedInPage).toHaveURL(/\/request\/detail\/[\w-]+/)

    const url = loggedInPage.url()
    const match = url.match(/\/request\/detail\/([\w-]+)/)
    expect(match).not.toBeNull()
    const requestId = match![1]

    await detail.expectStatus('DRAFT')
    await detail.submit()
    await detail.expectStatus('SUBMITTED')

    await detail.assignTo('engineer1')
    await detail.expectStatus('ASSIGNED')

    // Re-navigate to confirm the status actually persisted (not optimistic UI).
    await detail.goto(requestId)
    await detail.expectStatus('ASSIGNED')
  })

  test('list page shows submitted requests', async ({ loggedInPage }) => {
    const list = new RequestListPage(loggedInPage)
    await list.goto()
    await list.filterByStatus('SUBMITTED')
    await expect(loggedInPage.locator('.ant-table')).toBeVisible()
  })
})
