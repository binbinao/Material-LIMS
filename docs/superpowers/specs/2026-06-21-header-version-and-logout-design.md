# Header 版本徽标 + 退出按钮修复 — Design

- **Date:** 2026-06-21
- **Author:** Claude (brainstorming session)
- **Status:** Draft (pending user review)
- **Scope:** Frontend only (`lims-web-ui`). Backend untouched.

## 1. Goal

Two related defects in the top-right header of the Material LIMS frontend:

1. **Missing logout button.** After login, the user has no visible way to sign out. The `UserMenu` component is wired into `actionsRender`, but it is not rendered (or is rendered invisibly) because:
   - `.umirc.ts` configures `actionsRender` but does **not** set `layout.logout`. The Umi-generated `rightRender.tsx` therefore skips the dropdown wrapper around the avatar, and only `SelectLang` is rendered in the default avatar slot.
   - The `UserMenu` button may also be styled out by the ProLayout's actions area.

2. **No version identifier anywhere.** Operators and testers have no in-app way to confirm which build they are looking at.

## 2. Non-goals

- Backend changes. No new endpoints, no version controller, no actuator surface.
- E2E test scaffolding. The manual acceptance checklist (section 8) replaces a test suite for this iteration.
- Refactoring `UserMenu`, `utils/auth.ts`, or `services/requestService.ts` beyond what is required to make the button visible and confirm before logout.

## 3. Current state (verified 2026-06-21)

| File | Contents | Relevance |
|---|---|---|
| `lims-web-ui/package.json` | `"version": "1.0.0"` | Source of truth for frontend version. |
| `pom.xml` (root) | `<version>1.0.0-SNAPSHOT</version>` | Backend version, shown only in tooltip for context. |
| `lims-web-ui/.umirc.ts` | `actionsRender` returns `[<UserMenu />]`; no `layout.logout` set | Cause of invisible logout. |
| `lims-web-ui/src/.umi/plugin-layout/rightRender.tsx` | Auto-generated. Line 23: `showAvatar = initialState.avatar || initialState.name || runtimeConfig.logout`. With no `runtimeConfig.logout` and no `initialState.name` (only `initialState.currentUser.name`), the avatar slot is empty. | Confirms why nothing renders in the default slot. |
| `lims-web-ui/src/components/UserMenu/index.tsx` | Dropdown with user info + Logout item that calls `utils/auth.ts#logout()`. | Component exists, just not visible. |
| `lims-web-ui/src/utils/auth.ts` | `logout()` — POSTs `/api/v1/auth/logout`, removes `dev_user` from localStorage, `window.location.href = '/login'`. | Working as-is. |
| `lims-web-ui/src/services/requestService.ts` | `logout()` — wraps the `fetch` to `/api/v1/auth/logout`. | Working as-is. |
| `lims-web-ui/src/access.ts` | Returns role flags from `currentUser.roles`. Does **not** gate `/login` route. | Logout → /login → page renders; access plugin does not bounce. |

## 4. Design

### 4.1 `.umirc.ts` — define APP_VERSION, inject VersionBadge, expose logout

Three changes:

1. **Inject version via `define`**: Umi's `define` injects build-time constants into the bundle. We import `package.json` and stringify its `version` so the bundle contains the literal at build time.

   ```ts
   import pkg from './package.json';
   // ...
   define: {
     'process.env.APP_VERSION': JSON.stringify(pkg.version),
   },
   ```

2. **`actionsRender` returns `[<VersionBadge />, <UserMenu />]`** so the badge appears to the **left** of the user button. ProLayout renders `actionsRender` items in array order, so the badge (index 0) is left of the user button (index 1).

3. **`layout.logout = async () => { const { logout } = await import('./src/utils/auth'); await logout(); }`**. Setting this is a belt-and-suspenders measure: even if a future Umi upgrade changes `rightRender.tsx`, the built-in dropdown will still wrap the default avatar with our real logout handler. `UserMenu` remains as the primary, explicitly-styled logout trigger.

### 4.2 `src/components/VersionBadge/index.tsx` — new file

A pure-presentational component:

- Renders Ant Design `<Tag color="blue" style={{ cursor: 'default' }}>v{APP_VERSION}</Tag>`.
- Wrapped in `<Tooltip title={...}>`. The `title` prop is a React fragment of three `<div>` elements (one per line) so Ant Design renders them as separate rows instead of a single string with literal `\n` characters:
  ```tsx
  <Tooltip
    title={
      <>
        <div>Material LIMS Frontend</div>
        <div>Backend {BACKEND_VERSION}</div>
        <div>Build {BUILD_DATE}</div>
      </>
    }
  >
  ```
- Reads `process.env.APP_VERSION` (defined by Umi at build time; falls back to `'0.0.0'` in jest / outside Umi where the define has not run).
- Backend version and build date are imported from a sibling `src/buildInfo.ts` constants file so they live in one place and can be updated without touching the component. The build date is set to the spec date (`2026-06-21`) and acknowledged as "the date this version was introduced"; it is **not** auto-generated at build time (out of scope).

### 4.3 `src/buildInfo.ts` — new file

```ts
/** Backend version, kept in sync with pom.xml. Updated manually on bump. */
export const BACKEND_VERSION = '1.0.0-SNAPSHOT';
/** Date this frontend bundle was introduced. */
export const BUILD_DATE = '2026-06-21';
```

### 4.4 `src/components/UserMenu/index.tsx` — confirm before logout

The `Logout` item's `onClick` becomes:

```ts
onClick: () => {
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
},
```

`Modal` is imported from `antd` (already in deps). No new dependencies. `Modal.confirm` is used (not a stateful `useState` + `<Modal>`) so the component stays a single render with no extra mount/unmount churn.

### 4.5 Files NOT changed

- `src/utils/auth.ts` — already correct.
- `src/services/requestService.ts` — already correct.
- `src/app.tsx` — already calls `getInitialState` correctly. The 401 redirect in `errorHandler` already sends users to `/login` if their session expires, which is the right behavior on logout too.
- `src/access.ts` — does not gate `/login`, so logout → `/login` works without changes.

## 5. Visual result

```
+----------------------------------------------------------------+
|  Material LIMS            [v1.0.0]   [👤 张三 ▾]              |
|                                            ├── 张三            |
|                                            ├── ROLE_REQUESTER  |
|                                            ├── ───────────     |
|                                            └── Logout          |
+----------------------------------------------------------------+
```

The blue `v1.0.0` tag sits immediately to the left of the user button. Hovering it shows the tooltip with backend version + build date. Clicking the user button reveals the existing dropdown; clicking Logout now triggers a confirmation Modal first.

## 6. Risks

| Risk | Mitigation |
|---|---|
| Umi's `define` only inlines at build time — `process.env.APP_VERSION` is `undefined` in jest. | VersionBadge falls back to `'0.0.0'` when undefined; existing unit tests for unrelated components won't load VersionBadge. |
| Adding `layout.logout` AND keeping `actionsRender` may double-render. | `rightRender.tsx` only wraps the default avatar (which is empty in our case) when `runtimeConfig.logout` is set. `actionsRender` is a separate slot. No overlap. |
| `Modal.confirm` is a static call — in React 18 StrictMode dev double-invoke, the modal opens twice. | Confirmed by Ant Design docs: `Modal.confirm` is idempotent in v5.x and uses an internal ref. We do not run StrictMode in this app. |
| `BACKEND_VERSION` drifts from `pom.xml` if someone bumps the backend and forgets. | Acceptable for v1. Build info is informational, not enforced. Add a follow-up: backend `/actuator/info` if drift becomes a real problem. |

## 7. Dependencies

- `antd` `Modal`, `Tag`, `Tooltip` — already in `dependencies`.
- No new packages.

## 8. Acceptance checklist

Manual verification by the user after implementation:

1. `cd lims-web-ui && npm run dev`.
2. Log in as any dev user (the dev quick-login page accepts any name).
3. Top-right header shows, left-to-right: `v1.0.0` blue tag, then `👤 张三` button.
4. Hover the `v1.0.0` tag → tooltip shows "Material LIMS Frontend / Backend 1.0.0-SNAPSHOT / Build 2026-06-21".
5. Click `👤 张三` → dropdown shows display name, role, divider, Logout.
6. Click Logout → Modal "确认退出登录？" appears with 退出 / 取消 buttons.
7. Click 取消 → Modal closes, header still shows user.
8. Click Logout again, then 退出 → page redirects to `/login`, header is now empty (no badge, no user button).
9. Refresh → still on `/login`. localStorage has no `dev_user` key (verify in DevTools).
10. `npm run lint` passes. `npm run build` succeeds.

## 9. Out of scope / follow-ups

- Backend `/api/v1/system/version` endpoint returning `{frontend, backend, buildTime}`.
- Real `BUILD_DATE` injection via `date -u +%Y-%m-%d` shell step in a future CI pipeline.
- E2E test (`UserMenu → Modal → /login`).
- Replace dev-quick-login with real Azure AD flow (the JWT + cookie path already exists in `utils/auth.ts` and `services/requestService.ts`).

## 10. Changelog

- 2026-06-21: Initial draft.