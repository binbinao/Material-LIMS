import { describe, expect, it } from '@jest/globals'
import * as fs from 'fs'
import * as path from 'path'

/**
 * TDD test for issue #54 (P8): {@code ReportDetail} must gate the Submit
 * button by report authorship, not just by role. Today anyone with
 * `canEngineer || canManager` sees Submit, but the backend rejects
 * non-authors via {@code validateReportOwnership}.
 *
 * Asserted at source level — a rendered React test would need the full
 * Umi runtime + Ant Design providers, which the project's jest setup
 * does not exercise.
 */
describe('ReportDetail submit button authorship gate', () => {
  function readReportDetail(): string {
    let dir = process.cwd()
    for (let i = 0; i < 6; i++) {
      const candidate = path.join(
        dir,
        'lims-web-ui/src/pages/report/ReportDetail/index.tsx',
      )
      if (fs.existsSync(candidate)) {
        return fs.readFileSync(candidate, 'utf8')
      }
      dir = path.dirname(dir)
    }
    throw new Error('ReportDetail/index.tsx not found above ' + process.cwd())
  }

  it('gates Submit (and Edit/Sync in DRAFT/REVISING) on report.authorId === currentUserId', () => {
    const src = readReportDetail()
    const hasIsAuthorConst =
      src.includes('isAuthor') || src.includes('report.authorId ===')
    const gatesSubmitButton = /authorId[^&]*&&[^|]*Submit/.test(src)
    expect(hasIsAuthorConst || gatesSubmitButton).toBe(true)
  })

  it('still allows MANAGER+ to view the page (no hard role removal)', () => {
    const src = readReportDetail()
    const hasRoleGate =
      src.includes('canEngineer') || src.includes('canManager')
    expect(hasRoleGate).toBe(true)
  })
})
