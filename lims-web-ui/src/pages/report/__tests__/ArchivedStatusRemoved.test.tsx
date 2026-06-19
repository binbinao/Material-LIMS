import { describe, expect, it } from '@jest/globals'
import * as fs from 'fs'
import * as path from 'path'

/**
 * TDD test for issue #62 (P6): the three frontend reportStatusMap
 * definitions must NOT include an ARCHIVED key, since the backend
 * ReportStatus enum + DB CHECK constraint don't allow it (only DRAFT,
 * IN_REVIEW, APPROVED, REVISING).
 *
 * Asserted at source level — no React render needed.
 */
describe('Report status map has no ARCHIVED entry', () => {
  function read(rel: string): string {
    let dir = process.cwd()
    for (let i = 0; i < 6; i++) {
      const candidate = path.join(dir, rel)
      if (fs.existsSync(candidate)) {
        return fs.readFileSync(candidate, 'utf8')
      }
      dir = path.dirname(dir)
    }
    throw new Error(rel + ' not found above ' + process.cwd())
  }

  it('ReportList/index.tsx omits ARCHIVED', () => {
    const src = read('lims-web-ui/src/pages/report/ReportList/index.tsx')
    expect(src).not.toMatch(/ARCHIVED\s*:\s*\{/)
  })

  it('ReportDetail/index.tsx omits ARCHIVED', () => {
    const src = read('lims-web-ui/src/pages/report/ReportDetail/index.tsx')
    expect(src).not.toMatch(/ARCHIVED\s*:\s*\{/)
  })

  it('ReportRevisions/index.tsx omits ARCHIVED', () => {
    const src = read('lims-web-ui/src/pages/report/ReportRevisions/index.tsx')
    expect(src).not.toMatch(/ARCHIVED\s*:\s*\{/)
  })
})
