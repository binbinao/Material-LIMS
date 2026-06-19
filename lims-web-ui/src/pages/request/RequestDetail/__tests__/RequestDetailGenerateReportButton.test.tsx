import { describe, expect, it } from '@jest/globals'
import * as fs from 'fs'
import * as path from 'path'

/**
 * TDD test for issue #56 (P2): {@code RequestDetail} must expose a way to
 * trigger {@code createReport()}. Today the backend createReport endpoint
 * is reachable from Swagger / curl, but no UI button calls it — the
 * Report workflow is unreachable from normal user interaction.
 *
 * The fix: add a "Generate Report" button in the request workflow card
 * (or extra slot) when status is REPORTING/APPROVING/COMPLETED and
 * caller is ENGINEER/MANAGER/ADMIN.
 *
 * Asserted at source level — no React render here.
 */
describe('RequestDetail Generate Report button', () => {
  function readRequestDetail(): string {
    let dir = process.cwd()
    for (let i = 0; i < 6; i++) {
      const candidate = path.join(
        dir,
        'lims-web-ui/src/pages/request/RequestDetail/index.tsx',
      )
      if (fs.existsSync(candidate)) {
        return fs.readFileSync(candidate, 'utf8')
      }
      dir = path.dirname(dir)
    }
    throw new Error('RequestDetail/index.tsx not found above ' + process.cwd())
  }

  it('imports createReport from the service module', () => {
    const src = readRequestDetail()
    expect(src).toMatch(
      /import\s*\{[^}]*\bcreateReport\b[^}]*\}\s*from\s*['"]@\/services\/requestService['"]/,
    )
  })

  it('renders a "Generate Report" button (text or key) for REPORTING+ status', () => {
    const src = readRequestDetail()
    const hasText =
      /Generate Report|生成报告|Create Report|新建报告/i.test(src)
    const hasKey = /key=["']\w*report\w*["']/i.test(src)
    expect(hasText || hasKey).toBe(true)
  })

  it('gates the button on canEngineer or canManager', () => {
    const src = readRequestDetail()
    const hasRoleGate =
      /canEngineer\s*\|\|\s*access\.canManager|canManager\s*\|\|\s*access\.canEngineer/.test(
        src,
      )
    expect(hasRoleGate).toBe(true)
  })
})
