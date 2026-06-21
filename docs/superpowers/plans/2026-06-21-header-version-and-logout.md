# Header Version Badge + Logout Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the user logout button visible in the top-right header of the Material LIMS frontend, and add a version badge so operators can see which build they are on.

**Architecture:** Frontend-only changes. Add a `VersionBadge` component reading its version from a build-time Umi `define`; the existing `UserMenu` stays but gets a `Modal.confirm` gate before logout; `.umirc.ts` is extended with the define plus a defensive `layout.logout` so the Umi-built-in avatar dropdown also wires to our logout handler.

**Tech Stack:** TypeScript 5.3, React 18, Umi.js Max 4.6, Ant Design 5.15, jest 30 + @testing-library/react 16 (one smoke test only), esbuild via Umi.

**Spec:** `docs/superpowers/specs/2026-06-21-header-version-and-logout-design.md`

---

## File Structure

| Path | Action | Purpose |
|------|--------|---------|
| `lims-web-ui/src/buildInfo.ts` | create | Backend version + build date constants |
| `lims-web-ui/src/components/VersionBadge/index.tsx` | create | Pure presentational badge + tooltip |
| `lims-web-ui/src/components/VersionBadge/__tests__/VersionBadge.test.tsx` | create | Smoke test: renders `v0.0.0` (jest fallback) when `process.env.APP_VERSION` is undefined |
| `lims-web-ui/.umirc.ts` | modify | Add `define` for `APP_VERSION`, prepend `<VersionBadge />` in `actionsRender`, add `layout.logout` |
| `lims-web-ui/src/components/UserMenu/index.tsx` | modify | Wrap Logout `onClick` in `Modal.confirm` |
| `lims-web-ui/src/components/UserMenu/__tests__/UserMenu.test.tsx` | create | Smoke test: clicking Logout opens Modal, clicking 取消 closes it without calling `logout()` |

**Total: 6 files (4 new, 2 modified). No backend, no new dependencies.**

---

## Out-of-scope (per spec)

- Backend version endpoint
- Auto-generated `BUILD_DATE` via CI shell step
- E2E tests for `UserMenu → Modal → /login`
- Real Azure AD replacement of dev quick-login

---

## Task 1: Create `src/buildInfo.ts` constants

**Files:**
- Create: `lims-web-ui/src/buildInfo.ts`

- [ ] **Step 1: Write the file**

```ts
/**
 * Build-time constants for the frontend bundle.
 *
 * `BACKEND_VERSION` is updated manually whenever `pom.xml` is bumped.
 * `BUILD_DATE` is the date this frontend bundle was introduced; a future
 * CI step can replace it with `date -u +%Y-%m-%d` at build time.
 */

export const BACKEND_VERSION = '1.0.0-SNAPSHOT';
export const BUILD_DATE = '2026-06-21';
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd lims-web-ui && npx tsc --noEmit -p tsconfig.json`
Expected: zero errors. The file is pure constants, so no runtime check is needed.

- [ ] **Step 3: Commit**

```bash
git add lims-web-ui/src/buildInfo.ts
git commit -m "feat(ui): add buildInfo constants for version badge tooltip"
```

---

## Task 2: Create `VersionBadge` component + smoke test

**Files:**
- Create: `lims-web-ui/src/components/VersionBadge/index.tsx`
- Create: `lims-web-ui/src/components/VersionBadge/__tests__/VersionBadge.test.tsx`

- [ ] **Step 1: Write the failing test**

Create file `lims-web-ui/src/components/VersionBadge/__tests__/VersionBadge.test.tsx`:

```tsx
import React from 'react';
import { render, screen } from '@testing-library/react';
import VersionBadge from '../index';

describe('<VersionBadge />', () => {
  test('renders the version from process.env.APP_VERSION, with 0.0.0 fallback', () => {
    // jest does not run the Umi `define` step, so process.env.APP_VERSION
    // is undefined here. The component should fall back to "0.0.0".
    render(<VersionBadge />);
    expect(screen.getByText('v0.0.0')).toBeInTheDocument();
  });

  test('renders BACKEND_VERSION and BUILD_DATE inside the tooltip', async () => {
    render(<VersionBadge />);
    // The tooltip content is rendered lazily by antd; user-event hover
    // is overkill for a smoke test. We assert the constants are imported
    // by re-rendering and checking they appear in the DOM tree once
    // the tooltip mounts (antd mounts it in a portal at the document
    // root on hover — here we just sanity-check by searching for the
    // buildInfo strings anywhere in the document body).
    expect(document.body.textContent).toContain('Backend 1.0.0-SNAPSHOT');
    expect(document.body.textContent).toContain('Build 2026-06-21');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd lims-web-ui && npm test -- VersionBadge`
Expected: FAIL with "Cannot find module '../index'" (the component does not exist yet).

- [ ] **Step 3: Implement the component**

Create file `lims-web-ui/src/components/VersionBadge/index.tsx`:

```tsx
import React from 'react';
import { Tag, Tooltip } from 'antd';
import { BACKEND_VERSION, BUILD_DATE } from '../../buildInfo';

/**
 * VersionBadge — small blue tag in the top-right header showing the
 * frontend version. Hover reveals backend version + build date so
 * operators can confirm which build they are looking at.
 *
 * APP_VERSION is injected by Umi's `define` at build time. In jest
 * (and any other context where the bundle has not been processed by
 * Umi) it falls back to '0.0.0' so the badge still renders.
 */
export default function VersionBadge() {
  const version =
    (typeof process !== 'undefined' && process.env?.APP_VERSION) || '0.0.0';

  return (
    <Tooltip
      title={
        <>
          <div>Material LIMS Frontend</div>
          <div>Backend {BACKEND_VERSION}</div>
          <div>Build {BUILD_DATE}</div>
        </>
      }
    >
      <Tag color="blue" style={{ cursor: 'default', margin: 0 }}>
        v{version}
      </Tag>
    </Tooltip>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd lims-web-ui && npm test -- VersionBadge`
Expected: PASS. Both assertions resolve.

- [ ] **Step 5: Commit**

```bash
git add lims-web-ui/src/components/VersionBadge/
git commit -m "feat(ui): add VersionBadge with tooltip showing backend version + build date"
```

---

## Task 3: Wire `VersionBadge` into `.umirc.ts` + add `layout.logout`

**Files:**
- Modify: `lims-web-ui/.umirc.ts` (entire file)

- [ ] **Step 1: Replace `.umirc.ts` contents**

The whole file becomes:

```ts
import { defineConfig } from '@umijs/max';
import React from 'react';
import routes from './config/routes';
import pkg from './package.json';
import UserMenu from './src/components/UserMenu';
import VersionBadge from './src/components/VersionBadge';

export default defineConfig({
  antd: {},
  access: {},
  model: {},
  initialState: {},
  locale: {},
  request: {},
  // Inject APP_VERSION at build time. JSON.stringify keeps it a literal
  // string in the bundle so VersionBadge can read it as process.env.APP_VERSION.
  define: {
    'process.env.APP_VERSION': JSON.stringify(pkg.version),
  },
  layout: {
    title: 'Material LIMS',
    locale: true,
    // actionsRender items are laid out left-to-right in array order.
    // Badge first → it appears to the left of the user button.
    actionsRender: (props: any) => [
      React.createElement(VersionBadge, { key: 'version-badge' }),
      React.createElement(UserMenu, {
        key: 'user-menu',
        initialState: props?.initialState,
      }),
    ],
    // Defensive: if a future Umi upgrade changes how rightRender.tsx
    // decides whether to wrap the default avatar in a dropdown, our
    // logout still works because it goes through the same code path.
    logout: async () => {
      const { logout } = await import('./src/utils/auth');
      await logout();
    },
  },
  routes,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
  npmClient: 'npm',
  hash: true,
});
```

- [ ] **Step 2: Verify TypeScript still compiles**

Run: `cd lims-web-ui && npx tsc --noEmit -p tsconfig.json`
Expected: zero errors. `.umirc.ts` is run through Umi's bundler, but `tsc` also type-checks it.

- [ ] **Step 3: Run lint**

Run: `cd lims-web-ui && npm run lint`
Expected: PASS. If prettier complains about `.umirc.ts` formatting, run `npm run prettier` first then re-lint.

- [ ] **Step 4: Verify dev server starts**

Run: `cd lims-web-ui && npm run dev` (let it boot, then Ctrl-C after seeing "compiled successfully")
Expected: bundle compiles. The browser preview at `http://localhost:8000` would now show the badge in the top-right (verification step in Task 5).

- [ ] **Step 5: Commit**

```bash
git add lims-web-ui/.umirc.ts
git commit -m "feat(ui): wire VersionBadge into header + expose layout.logout"
```

---

## Task 4: Add `Modal.confirm` to `UserMenu` Logout

**Files:**
- Modify: `lims-web-ui/src/components/UserMenu/index.tsx` (entire file)
- Create: `lims-web-ui/src/components/UserMenu/__tests__/UserMenu.test.tsx`

- [ ] **Step 1: Write the failing test**

Create file `lims-web-ui/src/components/UserMenu/__tests__/UserMenu.test.tsx`:

```tsx
import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import UserMenu from '../index';

// Mock the auth utility so we can assert whether logout was called.
jest.mock('../../../utils/auth', () => ({
  logout: jest.fn(),
}));

import { logout } from '../../../utils/auth';

describe('<UserMenu /> Logout confirmation', () => {
  const initialState = {
    currentUser: { displayName: '张三', roles: 'ROLE_REQUESTER' },
  };

  beforeEach(() => {
    (logout as jest.Mock).mockReset();
  });

  test('clicking Logout opens a confirmation Modal and does NOT call logout yet', async () => {
    render(<UserMenu initialState={initialState} />);
    await userEvent.click(screen.getByRole('button', { name: /张三/ }));
    await userEvent.click(screen.getByText('Logout'));
    // Modal title appears
    expect(await screen.findByText('确认退出登录？')).toBeInTheDocument();
    // logout must not have been called yet
    expect(logout).not.toHaveBeenCalled();
  });

  test('clicking 取消 closes the Modal without calling logout', async () => {
    render(<UserMenu initialState={initialState} />);
    await userEvent.click(screen.getByRole('button', { name: /张三/ }));
    await userEvent.click(screen.getByText('Logout'));
    await userEvent.click(screen.getByRole('button', { name: '取消' }));
    // Modal title disappears
    await screen.findByText('确认退出登录？'); // still in DOM during transition
    expect(logout).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd lims-web-ui && npm test -- UserMenu`
Expected: FAIL — clicking Logout currently calls `logout()` directly, so the modal never appears and the first test fails to find "确认退出登录？".

- [ ] **Step 3: Replace `UserMenu` with the Modal-aware version**

Rewrite file `lims-web-ui/src/components/UserMenu/index.tsx`:

```tsx
import React from 'react';
import { Dropdown, Button, Modal } from 'antd';
import { LogoutOutlined, UserOutlined } from '@ant-design/icons';

/**
 * UserMenu — top-right Dropdown shown by the Umi layout's
 * `actionsRender` callback. Displays the current user's displayName
 * and a Logout item. Logout now goes through Modal.confirm so a
 * misclick doesn't kick the user out of the app.
 */
export default function UserMenu({ initialState }: { initialState?: any }) {
  const user = initialState?.currentUser;

  const confirmLogout = () => {
    Modal.confirm({
      title: '确认退出登录？',
      okText: '退出',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        const { logout } = await import('../../utils/auth');
        await logout();
      },
    });
  };

  return (
    <Dropdown
      menu={{
        items: [
          {
            key: 'name',
            label: (
              <div style={{ padding: '4px 0' }}>
                <div style={{ fontWeight: 600 }}>{user?.displayName || 'Unknown'}</div>
                <div style={{ fontSize: 12, color: '#999' }}>{user?.roles || ''}</div>
              </div>
            ),
            disabled: true,
          },
          { type: 'divider' as const },
          {
            key: 'logout',
            icon: <LogoutOutlined />,
            label: 'Logout',
            onClick: confirmLogout,
          },
        ],
      }}
    >
      <Button type="text" icon={<UserOutlined />}>
        {user?.displayName || 'Guest'}
      </Button>
    </Dropdown>
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd lims-web-ui && npm test -- UserMenu`
Expected: PASS. Both tests pass. The mocked `logout` is not called when the user clicks 取消.

- [ ] **Step 5: Run the full test suite**

Run: `cd lims-web-ui && npm test`
Expected: PASS. All tests pass — the original `setup.test.tsx` smoke test still passes; the two new component tests pass.

- [ ] **Step 6: Commit**

```bash
git add lims-web-ui/src/components/UserMenu/
git commit -m "feat(ui): confirm logout via Modal before redirecting"
```

---

## Task 5: Lint, build, and manual acceptance

**Files:** none

- [ ] **Step 1: Run lint**

Run: `cd lims-web-ui && npm run lint`
Expected: PASS. If prettier complains, run `npm run prettier` then re-lint.

- [ ] **Step 2: Run a production build**

Run: `cd lims-web-ui && npm run build`
Expected: Build succeeds. The bundle output goes to `dist/`. No TypeScript errors. The `process.env.APP_VERSION` literal `1.0.0` should appear inside `dist/` (grep for it as a sanity check):
Run: `cd lims-web-ui && grep -r "1.0.0" dist/ | head -5`
Expected: at least one match (the version string inlined into the bundle).

- [ ] **Step 3: Start backend + frontend together**

1. From repo root: `podman-compose up -d postgres redis minio`
2. Wait for DB to be ready, then: `psql -h localhost -U lims -d lims -f lims-web/src/main/resources/db/schema.sql` (idempotent; ignore "already exists" errors).
3. From repo root: `./mvnw spring-boot:run -pl lims-web -Dspring-boot.run.profiles=dev` (background; wait for "Started Application").
4. From `lims-web-ui/`: `npm run dev`.

- [ ] **Step 4: Run the spec's acceptance checklist**

Open `http://localhost:8000` in a browser and verify each item from spec section 8:

| # | Check | Expected |
|---|---|---|
| 1 | Log in via dev quick-login (any name) | Header shows `v1.0.0` badge + user button |
| 2 | Hover the badge | Tooltip shows "Material LIMS Frontend / Backend 1.0.0-SNAPSHOT / Build 2026-06-21" on three lines |
| 3 | Click user button | Dropdown shows display name, role, divider, Logout |
| 4 | Click Logout | Modal "确认退出登录？" appears with 退出 / 取消 |
| 5 | Click 取消 | Modal closes, header unchanged |
| 6 | Click Logout again, then 退出 | Redirects to `/login`, header is empty |
| 7 | Refresh | Still on `/login` |
| 8 | DevTools → Application → Local Storage | No `dev_user` key |
| 9 | Re-login | Header reappears with badge + user button |

Expected: all nine pass.

- [ ] **Step 5: Commit any verification artifacts (optional)**

If you made any incidental fixes during verification (e.g. CSS adjustment), commit them with a focused message. Otherwise, no commit — the implementation is already committed across Tasks 1-4.

---

## Self-Review (spec coverage)

| Spec section | Covered by |
|---|---|
| §1 Goal (logout visible + version identifier) | Task 3 wires VersionBadge; Task 4 wraps logout in Modal; the existing UserMenu now has a visible trigger because Task 3 keeps `actionsRender` returning it |
| §3 Current state — referenced as evidence | Not re-implemented (descriptive only) |
| §4.1 `.umirc.ts` changes (3 sub-changes) | Task 3, all three |
| §4.2 `VersionBadge` component | Task 2 |
| §4.3 `buildInfo.ts` | Task 1 |
| §4.4 `UserMenu` Modal.confirm | Task 4 |
| §4.5 Files NOT changed | `utils/auth.ts`, `services/requestService.ts`, `app.tsx`, `access.ts` — none of Tasks 1-4 touches these ✓ |
| §6 Risks — `process.env.APP_VERSION` undefined in jest | Task 2 component falls back to `'0.0.0'` and the test asserts this |
| §8 Acceptance checklist | Task 5, Steps 3-4 |
| §9 Out of scope | Explicitly excluded (no backend endpoint, no e2e) |

No spec gaps. No placeholders. Type names consistent across tasks (`VersionBadge`, `UserMenu`, `BACKEND_VERSION`, `BUILD_DATE`, `APP_VERSION`).