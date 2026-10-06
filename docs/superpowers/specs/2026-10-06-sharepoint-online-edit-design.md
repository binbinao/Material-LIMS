# SharePoint 在线编辑接入 — Design

- **Date:** 2026-10-06
- **Author:** Claude (brainstorming session)
- **Status:** Draft (pending user review)
- **Scope:** Backend (`lims-service` + `lims-web`) + Frontend (`lims-web-ui`).
- **Out of scope (explicit):** Azure AD SSO end-to-end enablement (endpoints remain 410, intentionally disabled by product decision), Word/Excel generation enhancement, PPT/Outlook/OneDrive/Teams, self-hosted WOPI.

## 1. Goal

Close the loop on "edit a Material LIMS report in Microsoft 365 Online, then sync the edited version back into LIMS" — the loop that `docs/writing/2026-06-05-phase3-plan.md` listed as P3 and `docs/writing/2026-06-05-phase5-summary.md` marked as deferred ("requires real Azure AD + SharePoint reachability").

After this spec is implemented, when a user clicks **Online Edit** on a report:

1. The backend uploads the freshly generated `.docx` to a SharePoint document library under `Reports/{YYYY}/{MM}/`.
2. The backend stores `sharepoint_file_id` and `sharepoint_edit_url` on the `report` row.
3. The frontend opens the URL in an iframe, the user edits in Word for the web.
4. The user clicks **Sync from SharePoint**; the backend downloads the edited `.docx`, stores it locally, regenerates the PDF, and updates `file_url` / `pdf_url`.

The implementation is **code-complete** (real Graph SDK calls wired up) but **runtime-degrades to a mock** in environments where `azure.ad.enabled=false` or credentials are missing. The mock branch matches the current dev/demo behavior so the dev quick-login flow stays functional without any Azure tenant.

## 2. Non-goals

- Restoring Azure AD SSO login (`AuthController` Azure endpoints stay 410; see `lims-web/src/main/java/com/lims/web/controller/AuthController.java`).
- Strengthening Word/Excel generation (separate task).
- PowerPoint, Outlook mail, OneDrive personal, Teams notifications.
- Self-hosting a WOPI host (requires Windows Server + Office Online Server; incompatible with the cross-platform Podman/Linux posture of this repo).
- Adding per-report `locked_by` / `locked_at` columns. Concurrency is delegated to M365 itself (Word for the web presents co-editing or read-only fallback natively).
- Generating the SharePoint site / library / folders automatically on first run. The operator pre-creates the site + library once via the SharePoint admin UI and supplies the identifiers via configuration.

## 3. Current state (verified 2026-10-06)

| File | Contents | Relevance |
|---|---|---|
| `pom.xml` (root) | `dependencyManagement` declares `poi-tl 1.12.2`, `microsoft-graph 5.77.0`, `azure msal4j 1.14.2` | Versions for the SDKs we will use. |
| `lims-service/pom.xml` | Pulls `poi-tl`, `microsoft-graph`, `msal4j`, `nimbus-jose-jwt` | Module deps already present. |
| `lims-service/.../service/ReportService.java` | `createReport` / `reviseReport` generate docx via `ReportTemplateService` + `SampleReportBuilder`, upload to MinIO, convert PDF. `getEditUrl` returns `report.getSharepointEditUrl()` (always `null` today). `syncFromSharePoint` is a no-op except for the `demoEnabled` guard that throws `OPERATION_NOT_ALLOWED` in prod. | The seams we will inject Graph calls into. |
| `lims-service/.../service/sync/MicrosoftGraphClient.java` | MSAL4J client credentials flow + `RestTemplate` against `https://graph.microsoft.com/v1.0`. Methods: `getAccessToken`, `listUsers`, `listGroups`. Conditional on `azure.ad.enabled=true`. | Reuse `getAccessToken`; add `putDriveItemContent` and `getDriveItem`. |
| `lims-service/.../service/sync/AzureAdSyncService.java` | 30-min scheduled user/group sync. | Untouched. |
| `lims-model/.../entity/Report.java` | Fields: `sharepointFileId`, `sharepointEditUrl` | Entity columns already mapped. |
| `lims-web/src/main/resources/db/init.sql` (L86) / `V1__init.sql` (L320–322) / `schema-h2.sql` | `report.sharepoint_file_id VARCHAR(200)`, `report.sharepoint_edit_url VARCHAR(1000)` | DB columns already present. No migration needed. |
| `lims-web/.../controller/ReportController.java` | `GET /api/v1/reports/{id}/edit-url` (ENGINEER/MANAGER/ADMIN) returns `R<String>`; `POST /api/v1/reports/{id}/sync` calls `reportService.syncFromSharePoint` | Endpoints already exposed. |
| `lims-web/src/main/resources/application.yml` (L67–73) | `azure.ad.{tenant-id,client-id,client-secret,redirect-uri}` block; `minio.{endpoint,access-key,secret-key,bucket}` block | Existing config blocks. We add a new `sharepoint.*` block. |
| `lims-web-ui/src/pages/report/ReportDetail/index.tsx` | Calls `getReportEditUrl` (button → `window.open`) and `syncReportFromSharePoint` (button) | We will gate the **Online Edit** button on `report.sharepointEditUrl` being non-null and inject an i18n-aware empty-state. |
| `lims-web-ui/src/pages/report/ReportEdit/index.tsx` | Loads `getReportEditUrl(params.id)` → renders `<iframe src={editUrl}>` if present, otherwise a centered empty-state string | We extend the empty-state to distinguish "SharePoint not configured" from "mock preview" and add a localized message. |
| `lims-web-ui/src/services/requestService.ts` (L227–233) | `getReportEditUrl`, `syncReportFromSharePoint` | No change. |
| `lims-web-ui/src/locales/{en-US,zh-CN}.ts` | `common.success/fail`, `common.export/download` present; no `report.edit.*` keys yet | We add `report.edit.title` (already referenced; confirm), `report.edit.unavailable`, `report.edit.mockHint`, `report.detail.onlineEdit`. |
| `docs/design/material-lims-design.md` (L474–476, L776–782, L1272+, L1477–1484, L1499–1566) | Design doc already describes the loop we are closing (Graph → SharePoint → Office Online → sync). | Source of truth for path conventions. |
| `docs/writing/2026-06-05-phase3-plan.md` (L44–50) | P3: "**SharePointService**: upload docx → take `webUrl + &action=edit`; download latest; **syncFromSharePoint**: download → upload to MinIO → convert PDF → update `file_url / pdf_url`; **lock/unlock**: approve → checkout, revise → checkin" | Matches our scope except we are explicitly deferring lock/unlock to M365. |
| `docs/runbook/user-manual.md` (L46) | User manual already documents "Online Edit → iframe → Sync" UX | No change. |

## 4. Design

### 4.1 Architecture overview

```
                         (azure.ad.enabled=true)            (azure.ad.enabled=false OR creds missing)
                         ┌─────────────────────────┐          ┌──────────────────────────────┐
                         │  GraphSharePointClient   │          │  MockSharePointClient        │
                         │  (real Graph drive)     │   vs.    │  (deterministic in-memory)  │
                         └────────────┬────────────┘          └──────────────┬───────────────┘
                                      │ implements                        │ implements
                                      └─────────────► SharePointClient ◄──┘
                                                       │
                                                       │ uploadDocx / downloadDocx / getEditUrl / isEnabled
                                                       ▼
                                            ReportService (existing)
                                            ▼
                                            createReport / syncFromSharePoint / getEditUrl / reviseReport
```

The interface lives in `lims-service`. Two implementations are registered as Spring beans:

- `GraphSharePointClient` is gated by `@ConditionalOnProperty(sharepoint.enabled=true AND azure.ad.enabled=true)`.
- `MockSharePointClient` is gated by `@ConditionalOnMissingBean(SharePointClient.class)`.

`MockSharePointClient` reuses the locally-generated `.docx` from MinIO (`report.file_url`) so the mock behaves identically to a real upload-then-sync round-trip. The `sharepointEditUrl` it returns is a stable mock URL prefixed with `https://example.invalid/mock-sharepoint/{reportId}` so the frontend can detect "this is a mock" and avoid presenting it as a real Microsoft URL.

### 4.2 New classes

All paths are under `lims-service/src/main/java/com/lims/service/sharepoint/`.

```
com.lims.service.sharepoint
├── SharePointClient.java               interface
├── SharePointProperties.java           @ConfigurationProperties("sharepoint")
├── SharePointPathResolver.java         requestNo/version/createdAt → drive-relative path + filename
├── GraphSharePointClient.java          @Service, @ConditionalOnProperty azure.ad.enabled=true
└── MockSharePointClient.java           @Service, @ConditionalOnMissingBean(SharePointClient.class)
```

#### 4.2.1 `SharePointClient`

```java
public interface SharePointClient {
    boolean isEnabled();                                    // returns true for prod bean; mock always returns true in demo profile

    /** Upload the docx at localPath. If a driveItem with the same path already exists, Graph returns 409;
     *  we treat that as "already uploaded" and return the existing item rather than failing. */
    SharePointUploadResult uploadDocx(String localPath, String driveRelativePath, String filename);

    /** Download the docx bytes for sharepointFileId. */
    byte[] downloadDocx(String sharepointFileId);

    /** Compose the iframe-renderable edit URL. Graph impl returns driveItem.webUrl + "?action=edit".
     *  Mock impl returns a stable in-memory URL. */
    String composeEditUrl(String sharepointFileId);

    record SharePointUploadResult(String fileId, String editUrl) {}
}
```

Failure modes:
- `GraphSharePointClient` wraps every Graph failure in a new `SharePointException` (added in `lims-common`).
- `MockSharePointClient` never throws on the happy path. `downloadDocx` re-reads from MinIO via the existing `FileStorageService`; if the local file is missing it throws `SharePointException` (matches the prod impl).

#### 4.2.2 `SharePointProperties`

```java
@ConfigurationProperties(prefix = "sharepoint")
public record SharePointProperties(
    boolean enabled,
    String strategy,      // "folder_layout" | "flat"
    String hostname,      // e.g. "contoso.sharepoint.com"
    String sitePath,      // e.g. "/sites/lims"
    String library,       // e.g. "Documents"
    String folderRoot,    // e.g. "Reports"
    String driveId        // optional override; if non-empty, skip /sites resolution
) {}
```

`enabled` defaults to `false`. The two conditionals `azure.ad.enabled` and `sharepoint.enabled` both have to be `true` for the Graph impl to load; this is what gives ops an independent kill switch even when Azure creds are configured.

#### 4.2.3 `SharePointPathResolver`

```java
public record SharePointPath(String driveRelativePath, String filename) {}

public SharePointPath resolve(Request parent, Report report) {
    String requestNo = parent.getRequestNo();                   // e.g. "REQ-2026-0001"
    String version = report.getVersionNumber();                // e.g. "V1.0"
    String filename = requestNo + "_" + version + ".docx";
    String path = switch (properties.strategy()) {
        case "flat" -> "";
        default    -> "%s/%04d/%02d/".formatted(
                          properties.folderRoot(),
                          parent.getCreatedAt().getYear(),
                          parent.getCreatedAt().getMonthValue());
    };
    return new SharePointPath(path, filename);
}
```

The path strategy is hard-coded as enum-like; we do not parse arbitrary templates.

#### 4.2.4 `GraphSharePointClient`

Delegates auth to `MicrosoftGraphClient` (reusing `getAccessToken`). Three new methods, all `PUT` / `GET` against `https://graph.microsoft.com/v1.0`:

- `uploadDocx(localPath, driveRelativePath, filename)`:
  - Resolve drive id: if `properties.driveId()` is non-empty use it; else `GET /sites/{hostname}:/sites/{sitePath}?$select=id` → `GET /sites/{siteId}/drives?$filter=name eq '{library}'` → pick the first match.
  - `PUT /drives/{driveId}/items/children/{filename}:` is the wrong endpoint. Correct: `PUT /drives/{driveId}/root:/{driveRelativePath}{filename}:/content` with `Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document`. Body is the docx bytes.
  - On `409 Conflict` (item already exists), fall back to `GET /drives/{driveId}/root:/{path}{filename}` and return that driveItem.
  - Return `SharePointUploadResult(driveItem.id, driveItem.webUrl + "?action=edit")`.
- `downloadDocx(fileId)`: `GET /drives/{driveId}/items/{fileId}/content` with `Authorization: Bearer <token>`. Stream into `byte[]`. 4xx → `SharePointException`.
- `composeEditUrl(fileId)`: `GET /drives/{driveId}/items/{fileId}?$select=webUrl`, return `webUrl + "?action=edit"`. We compose lazily so the URL stays valid even if the host's edit suffix policy changes (we can update in one place).

Concurrency-safe token: `MicrosoftGraphClient.getAccessToken()` is already `synchronized` and caches for ~60s before the token expiry. We reuse it as-is.

### 4.3 Changes to existing classes

#### 4.3.1 `ReportService`

Three call sites change. The seam is always: **try the Graph call, on `SharePointException` log warn and continue**, so a flaky tenant cannot corrupt the local record.

- `createReport(reqId, authorId)`:
  - After `fileStorageService.upload(docx, "reports/" + requestId)` succeeds and before `reportMapper.updateById(report)`:
  - Call `sharePointClient.uploadDocx(docx.toString(), pathResolver.resolve(parent, report))`; on success, set `report.setSharepointFileId(...)` and `report.setSharepointEditUrl(...)` before the updateById.
  - Wrap in `try { ... } catch (SharePointException e) { log.warn("SharePoint upload failed (local report kept): {}", e.getMessage()); }`.
  - The mock bean always succeeds in `lims.demo.enabled=true`, so dev/demo UX matches prod.

- `getEditUrl(reportId)`:
  - Currently returns `report.getSharepointEditUrl()` directly.
  - Change to: if `report.getSharepointEditUrl()` is non-null, return `sharePointClient.composeEditUrl(report.getSharepointFileId())` (lets us lazily rebuild URLs if the suffix policy changes). If it is null, return null.

- `syncFromSharePoint(reportId)`:
  - Remove the `if (!demoEnabled)` `OPERATION_NOT_ALLOWED` throw.
  - Keep the `report == null` guard.
  - If `sharepointFileId` is null, log warn and return (no-op; matches the mock impl's behavior).
  - `byte[] bytes = sharePointClient.downloadDocx(report.getSharepointFileId())`.
  - Write bytes to a `Path` under `${java.io.tmpdir}/lims-sharepoint-sync/{reportId}/{uuid}.docx`, then `fileStorageService.upload(path, "reports/" + report.getRequestId())` → update `file_url`.
  - Reuse `wordToPdfConverter.convert(path)` → update `pdf_url` on success.
  - `reportMapper.updateById(report)`.

- `reviseReport(reportId, revisionNote, userId)`:
  - Same seam as `createReport`: after regenerating docx, call `sharePointClient.uploadDocx(...)` and update `sharepoint_file_id` / `sharepoint_edit_url`. Each revision is a new file (per the design decision matrix).

The `demoEnabled` flag stays in `ReportService` for `getSampleWordBytes` (issue #84). The SharePoint switch is purely the `SharePointClient.isEnabled()` + bean selection; we do not read `demoEnabled` in `createReport`.

#### 4.3.2 `MicrosoftGraphClient`

Add one method, used by `GraphSharePointClient`:

```java
public ResponseEntity<byte[]> getDriveItemContent(String driveId, String itemId) { ... }
public Map<String, Object> getDriveItem(String driveId, String itemId, String select) { ... }
public Map<String, Object> putDriveItemContent(String driveId, String parentPath, String filename, byte[] body, String contentType) { ... }
```

Each uses the same `Bearer` header from `getAccessToken()`. We do not introduce a new auth path.

#### 4.3.3 `Report` entity — **no change**

`sharepointFileId`, `sharepointEditUrl` already exist on `Report.java` (L33–34). DB columns already exist (V1__init.sql / init.sql / schema-h2.sql).

### 4.4 Configuration

#### 4.4.1 `application.yml` (root) — additions

```yaml
# SharePoint document library for report .docx uploads
sharepoint:
  enabled: ${SHAREPOINT_ENABLED:false}
  strategy: ${SHAREPOINT_STRATEGY:folder_layout}   # folder_layout | flat
  hostname: ${SHAREPOINT_HOSTNAME:}
  site-path: ${SHAREPOINT_SITE_PATH:/sites/lims}
  library: ${SHAREPOINT_LIBRARY:Documents}
  folder-root: ${SHAREPOINT_FOLDER_ROOT:Reports}
  drive-id: ${SHAREPOINT_DRIVE_ID:}
```

In `application-dev.yml` we keep `sharepoint.enabled: false` (the mock bean handles dev UX) and explicit comment that `azure.ad.enabled: false` already implies Graph impl disabled.

The `@ConditionalOnProperty` for `GraphSharePointClient` is:

```java
@ConditionalOnProperty(name = "sharepoint.enabled", havingValue = "true")
@ConditionalOnProperty(name = "azure.ad.enabled", havingValue = "true")
```

`MockSharePointClient` is `@ConditionalOnMissingBean(SharePointClient.class)`.

### 4.5 Frontend changes

#### 4.5.1 `ReportEdit/index.tsx`

Today:

```tsx
{loading ? <Spin />
  : editUrl ? <iframe src={editUrl} style={...} />
  : <div>{intl.formatMessage({ id: 'report.edit.title' })} {intl.formatMessage({ id: 'common.fail' })}</div>}
```

After:

```tsx
{loading ? <Spin />
  : !editUrl ? <EmptyCard text={intl.formatMessage({ id: 'report.edit.unavailable' })} />
  : isMockUrl(editUrl)
    ? <iframe src={editUrl} ... /> + <Alert type="info" message={intl.formatMessage({ id: 'report.edit.mockHint' })} />
    : <iframe src={editUrl} ... />}
```

`isMockUrl` is a small helper: `editUrl.startsWith('https://example.invalid/')`. The empty-state card reuses the existing `Card` and `<Empty />` from antd.

#### 4.5.2 `ReportDetail/index.tsx` — Online Edit button visibility

Today the action button grid calls `getReportEditUrl` then `window.open`. After:

- If `report.sharepointEditUrl` is null/undefined, render a disabled "Online Edit" button with tooltip "SharePoint not configured for this report". This replaces the current behavior of opening a blank iframe page.
- The `Sync from SharePoint` button keeps its current visibility rule (ENGINEER/MANAGER/ADMIN, status in {DRAFT, REVISING}).

#### 4.5.3 i18n additions

| Key | en-US | zh-CN |
|---|---|---|
| `report.edit.title` (existing; confirm present) | Edit Report | 编辑报告 |
| `report.edit.unavailable` | SharePoint online edit is not configured in this environment. | 当前环境未配置 SharePoint 在线编辑。 |
| `report.edit.mockHint` | Demo preview — your edits are not persisted to a real Microsoft 365 tenant. | 演示预览 — 编辑内容不会持久化到真实的 Microsoft 365 租户。 |
| `report.detail.onlineEdit` | Online Edit | 在线编辑 |
| `report.detail.sharepointUnavailable` | SharePoint online edit is not configured for this report. | 此报告未配置 SharePoint 在线编辑。 |

### 4.6 Error handling

| Failure | Behavior | User-visible result |
|---|---|---|
| Graph `401 Unauthorized` (bad tenant creds) | `SharePointException`, log warn, leave `sharepoint_file_id` null | Local report saved; Online Edit button disabled with tooltip. |
| Graph `403 Forbidden` (missing `Sites.ReadWrite.All` permission) | Same as 401 | Same. |
| Graph `404 Not Found` (library/site mis-configured) | Same | Same. |
| Graph `409 Conflict` on upload (file already exists) | Treated as success: fetch the existing driveItem and return its id | Same as success. |
| Graph `5xx` | Same as 401 | Same. |
| `sharepoint.enabled=false` (production with no integration) | `MockSharePointClient` is **not** loaded because `azure.ad.enabled=false` typically co-occurs; the bean selector uses `@ConditionalOnMissingBean`, so if neither bean matches, Spring fails to start. We add a fallback: a `NoOpSharePointClient` registered with `@ConditionalOnMissingBean(SharePointClient.class)` after the mock — returns null for `isEnabled`, throws `SharePointException` for any method call. | Local reports still generate; Online Edit button is disabled with the "not configured" tooltip. |
| `downloadDocx` fails during sync | `SharePointException`, log warn, do **not** update `file_url`/`pdf_url`. The report row keeps its old URLs. | User can retry sync. |
| Frontend gets `null` from `getReportEditUrl` | Frontend renders the new empty-state card | Clear UX. |

### 4.7 Testing

#### 4.7.1 Backend unit tests (`lims-service/src/test/java/...`)

- `MockSharePointClientTest`: asserts `isEnabled()` returns true in `dev` profile (no Spring context needed; the mock is pure Java); asserts `uploadDocx` followed by `composeEditUrl` round-trips to a URL prefixed with `https://example.invalid/mock-sharepoint/`; asserts `downloadDocx` reads from the local `file_url` via `FileStorageService` mock and throws `SharePointException` when the local file is missing.
- `GraphSharePointClientTest`: uses **WireMock** (added to `lims-service/pom.xml` test scope — see dependencies below) to stub `https://graph.microsoft.com/v1.0/sites/...`, `/drives/.../items/...:content`, and `/drives/.../items/{id}`. Covers:
  - Happy path: upload, get edit URL, download.
  - 401 from `/sites/{id}` → `SharePointException`.
  - 5xx from upload → `SharePointException`, log captured.
  - 409 on upload → falls back to GET existing item, returns success.
  - Missing drive id and `properties.driveId()` blank → resolves via site+drives query.
  - `downloadDocx` 404 → `SharePointException`.
- `ReportServiceTest` (extend the existing file if any, otherwise new): mocks `SharePointClient` to throw `SharePointException` from `uploadDocx`, asserts the local `Report` row still has `sharepoint_file_id == null` and `status == DRAFT`. Mocks success path and asserts the columns are populated.
- `SharePointPathResolverTest`: covers `folder_layout` with three sample `createdAt` values (Jan, Dec, leap-year Feb); covers `flat` strategy; asserts filename `{requestNo}_V{version}.docx`.

#### 4.7.2 Backend integration

Existing `lims-web/src/test/java/.../RequestControllerTest.java` style — boot the dev profile against H2, hit:

- `POST /api/v1/reports/requests/{id}/reports` → 200, response body has `sharepointFileId: "mock-{id}"` and `sharepointEditUrl: "https://example.invalid/mock-sharepoint/{id}?action=edit"`.
- `GET /api/v1/reports/{id}/edit-url` → 200 with the same URL.
- `POST /api/v1/reports/{id}/sync` → 200, `file_url` rewritten to a new MinIO path under `reports/{requestId}/`. (We do not assert against MinIO; we assert the URL is replaced.)

#### 4.7.3 Frontend

- Extend `ReportDetail/__tests__/buttons.test.tsx`: assert that when `report.sharepointEditUrl == null`, the Online Edit button is rendered but `disabled`, and a tooltip with the "SharePoint not configured" copy is attached (use `screen.getByRole('button', { name: ... })` and `aria-disabled`).
- Add `ReportEdit/__tests__/index.test.tsx` (new): mock `getReport` to return a report, `getReportEditUrl` to return `null` — assert the empty-state card is rendered with the i18n key text. Mock `getReportEditUrl` to return the mock URL — assert the iframe is rendered and a warning Alert is shown.
- Existing `report-lifecycle.spec.ts` E2E: extend to assert the iframe loads with the mock URL in dev.

### 4.8 Dependencies

#### 4.8.1 Backend

- `lims-service/pom.xml` test scope — add:
  ```xml
  <dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <version>3.10.0</version>
    <scope>test</scope>
  </dependency>
  ```
  (WireMock version pinned to match Spring Boot 3.2 compatibility; verify before merging.)
- No new runtime dependencies. `lims-common` already has a `BusinessException` + `ErrorCode` enum; we add `SharePointException` (subclass of `BusinessException` with `ErrorCode.SHAREPOINT_OPERATION_FAILED`, a new code we add to the enum).

#### 4.8.2 Frontend

- No new npm packages. antd already supplies `Empty`, `Alert`, `Tooltip`.

### 4.9 Files NOT changed

- `pom.xml` (root) — version pins are present, no new deps.
- `lims-model/.../entity/Report.java` — columns already mapped.
- DB migrations — columns already exist.
- `lims-web/.../controller/AuthController.java` — Azure SSO endpoints stay 410.
- `MicrosoftGraphClient` (existing methods) — kept as-is; new methods appended.
- `AzureAdSyncService` — untouched.
- `services/requestService.ts` — `getReportEditUrl` and `syncReportFromSharePoint` signatures unchanged.

## 5. Data flow

```
[Engineer]
  │
  │ POST /api/v1/reports/requests/{rid}/reports
  ▼
ReportController.create
  │
  ▼
ReportService.createReport
  ├── requestMapper.selectById(rid)         // parent status guard (existing)
  ├── ReportTemplateService.generate(...)     // → Path docx (existing)
  ├── fileStorageService.upload(docx, …)     // → report.fileUrl (existing)
  ├── SharePointPathResolver.resolve(parent, report)
  │     → SharePointPath("/Reports/2026/10/", "REQ-2026-0001_V1.0.docx")
  ├── SharePointClient.uploadDocx(localPath, path, filename)
  │     ├── Graph: PUT /drives/{driveId}/root:/Reports/2026/10/REQ-2026-0001_V1.0.docx:/content
  │     │   ↳ 201 { id, webUrl }
  │     └── report.setSharepointFileId(id); report.setSharepointEditUrl(webUrl + "?action=edit")
  ├── WordToPdfConverter.convert(docx)       // existing
  ├── fileStorageService.upload(pdf, …)       // → report.setPdfUrl
  └── reportMapper.updateById(report)         // one DB write carries fileUrl, pdfUrl, sharepointFileId, sharepointEditUrl
  │
  │ GET /api/v1/reports/{id}/edit-url
  ▼
ReportController.getEditUrl → ReportService.getEditUrl
  └── SharePointClient.composeEditUrl(sharepointFileId)
        └── Graph: GET /drives/{driveId}/items/{id}?$select=webUrl → webUrl + "?action=edit"
        │
        ▼
[Frontend] ReportEdit renders <iframe src={editUrl} />
[Engineer edits in Word for the web]
[Engineer clicks "Sync from SharePoint"]
  │
  │ POST /api/v1/reports/{id}/sync
  ▼
ReportController.syncFromSharePoint → ReportService.syncFromSharePoint
  ├── SharePointClient.downloadDocx(sharepointFileId)
  │     └── Graph: GET /drives/{driveId}/items/{id}/content → byte[]
  ├── write to ${java.io.tmpdir}/lims-sharepoint-sync/{id}/{uuid}.docx
  ├── fileStorageService.upload(path, "reports/{requestId}") → report.setFileUrl
  ├── WordToPdfConverter.convert(path) → report.setPdfUrl
  └── reportMapper.updateById(report)
```

## 6. Risks

| Risk | Mitigation |
|---|---|
| WireMock version mismatch with Spring Boot 3.2 test starter. | Verify with `./mvnw -pl lims-service test -Dtest='*SharePointClientTest*'` after the dep is in. If conflict, fall back to a hand-rolled `MockRestServiceServer` against the existing `RestTemplate`. |
| Microsoft Graph adds a new error class we don't recognize. | `SharePointException` wraps any non-2xx response; ops get one log line per failure with the status code and a truncated body. |
| `driveItem.webUrl` is sharepoint.com-host-specific; if a tenant uses `sharepoint.cn`, the URL suffix `?action=edit` may not behave as `?action=view` vs `?action=edit` differently. | Suffix is configurable via `sharepoint.edit-url-suffix` (default `?action=edit`). We add this as a single property rather than baking the assumption in. |
| Frontend `<iframe>` to `example.invalid` causes a console error / network failure. | The mock URL is rendered only inside the iframe; the parent page does not fetch it. The browser will show "this site can't be reached" inside the iframe, which is fine because we show the mock Alert above it. The mock Alert message tells the user this is a demo. |
| Dev profile uploads to MinIO with the mock branch but `file_url` ends up at a `https://example.invalid/...` URL; downstream code that assumes `file_url` is a MinIO path could break. | `report.file_url` (MinIO) and `report.sharepoint_edit_url` (M365) are separate columns; only the latter carries `example.invalid`. We do not change `FileStorageService` consumers. |
| `@ConditionalOnMissingBean` races when both `MockSharePointClient` and `NoOpSharePointClient` are candidates. | We annotate `NoOpSharePointClient` with `@ConditionalOnProperty(name = "sharepoint.enabled", havingValue = "false")` and `azure.ad.enabled=false` so it only loads when both flags are off. `MockSharePointClient` is gated by `@ConditionalOnProperty(name = "lims.demo.enabled", havingValue = "true")`. |
| The frontend's empty-state card accessibility: users on screen readers should hear the reason. | The card uses antd's `<Empty description={...} />`, which sets `role="status"` and `aria-live="polite"`. We add `aria-label={intl.formatMessage({ id: 'report.edit.unavailable' })}` for redundancy. |

## 7. Acceptance checklist

Verification after implementation:

1. `./mvnw -pl lims-common,lims-model,lims-dao,lims-service,lims-web -am test -Dtest='*SharePoint*'` — new unit tests pass.
2. `./mvnw -pl lims-web test -Dtest='ReportControllerTest'` — existing integration test extended, passes.
3. `cd lims-web-ui && npm test -- ReportEdit ReportDetail` — new + extended tests pass.
4. `cd lims-web-ui && npm run lint && npm run build` — no new lint errors, build succeeds.
5. `./mvnw -pl lims-web spring-boot:run -Dspring-boot.run.profiles=dev` + `cd lims-web-ui && npm run dev`:
     - Login as dev user, navigate to a report, click **Create Report**. Response includes `sharepointFileId: "mock-{id}"`.
     - Click **Online Edit**: opens `/report/{id}/edit`, renders the mock iframe + the demo Alert.
     - Click **Sync from SharePoint**: 200, report's `file_url` is rewritten; navigate to **Download PDF** still works.
     - Set `azure.ad.enabled=true` and `sharepoint.enabled=true` in a test config with stubbed Graph endpoints (WireMock running on `localhost:8089`): the same flow calls the stub and returns real-looking ids (`stub-{id}`).
6. Manual i18n check: switch to `zh-CN`, all new strings render in Chinese.
7. Manual accessibility check: with VoiceOver / NVDA, the Online Edit button announces its disabled state and the empty-state card announces the unavailable reason.

## 8. Out of scope / follow-ups

- Real Azure AD SSO login (`AuthController` endpoints remain 410; tracked separately).
- Optimistic concurrency lock on `Report` rows (delegated to M365 today).
- Auto-provisioning the SharePoint site / library / folders on first run. We assume ops creates them once.
- Documenting the operator runbook entry for setting `SHAREPOINT_*` env vars; tracked under `docs/runbook/uat-environment.md` updates.
- Switching the mock iframe to a richer preview (e.g. OnlyOffice) if M365 is unreachable in production. Out of scope; matches design doc L1484 "备选方案:OnlyOffice 自部署" being a separate decision.

## 9. Changelog

- 2026-10-06: Initial draft.