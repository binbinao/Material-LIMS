# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Summary

**Material LIMS** (材料实验室信息管理系统) — full-lifecycle LIMS for委托 (Request) → 制样 (Sample) → 报告 (Report) workflows. Java 17 / Spring Boot 3.2 multi-module Maven backend, Umi.js 4 / React 18 / Ant Design Pro 6 frontend, Azure AD SSO, Microsoft 365 Graph for online Word editing, Flowable 7 BPMN for workflow.

> A more comprehensive rule file exists at [`CODEBUDDY.md`](CODEBUDDY.md) with full command listings, the end-to-end request flow, and detailed cross-cutting concerns. Read it first when working in this repo — this file is a concise Claude-Code-oriented distillation.

## Tech Stack (locked versions — do not bump casually)

- **Backend**: Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6, Flowable 7.0.1, MapStruct 1.5.5, Lombok, Hutool 5.8.26, Resilience4j 2.2.0, springdoc-openapi 2.3.0
- **Frontend**: Umi.js Max 4.6, React 18, Ant Design 5 + Pro Components 2.6, ECharts 5, TypeScript 5.3, ESLint 8 + @typescript-eslint v7, Prettier 3
- **Infra**: PostgreSQL 15, Redis 7, MinIO 8.5 (object storage), poi-tl 1.12 (Word templating), Microsoft Graph 5.77, msal4j 1.14

## Module Layout (Maven multi-module, one-way dependencies)

```
lims-common   ← lims-model
lims-model    ← lims-dao
lims-common/model/dao ← lims-service
lims-common/model/service ← lims-workflow
lims-common ← lims-admin
lims-* 全部 ← lims-web   (启动入口 + Controller + 安全过滤器 + 全局异常)
```

Reverse dependencies are forbidden. Frontend lives in `lims-web-ui/` (Umi.js Max convention-based).

| Module | Responsibility |
|---|---|
| `lims-common` | `R<T>` response, `ErrorCode` enum (1xxx 通用 / 2xxx 业务 / 3xxx 鉴权 / 5xxx 系统), `BusinessException`, `@AuditLog`, `JwtTokenProvider`, `SecurityUtils`, `HolidayCalendar` |
| `lims-model` | All Entity / DTO / VO / Enum types — single source of truth for the data shape |
| `lims-dao` | MyBatis-Plus `BaseMapper` interfaces, one file per table |
| `lims-service` | Business logic, sub-packages: `basic`, `request`, `report`, `equipment`, `dashboard`, `storage`, `security`, `sync`, `integration`, `scheduler`, `config` |
| `lims-workflow` | Flowable engine wrapper + BPMN process definitions (`request-process.bpmn20.xml`, key=`requestProcess`) |
| `lims-admin` | User/role/audit-log/i18n management |
| `lims-web` | Spring Boot bootstrap, `Controller/`, `security/` (Jwt + DevAuth filters), `resources/db/{schema,init}.sql`, `application.yml` + `application-dev.yml` |

## Build, Run, Test

All commands run from the repo root unless noted.

### Backend (Maven wrapper)

```bash
# Build everything (skip tests for speed)
./mvnw clean package -DskipTests
./mvnw package -pl lims-web -am         # web + its deps only
./mvnw compile -pl lims-service -am     # incremental

# Local run (dev profile)
podman-compose stop lims-backend        # free port 8080
./mvnw spring-boot:run -pl lims-web

# Tests
./mvnw test                             # all modules
./mvnw test -pl <module>                # single module
./mvnw test -Dtest=ClassName            # by class
./mvnw test -Dtest=ClassName#method     # by method

# Container build (mirrors production)
podman-compose build lims-backend
podman-compose up -d postgres redis minio
```

### Frontend (`lims-web-ui/`)

```bash
npm install
npm run dev          # http://localhost:8000  (proxies /api → :8080 via .umirc.ts)
npm run build        # → dist/
npm run lint         # eslint + prettier --check
npm run lint:fix     # eslint --fix
npm run prettier     # prettier --write
npm test             # jest
```

> `@umijs/max` does not run `tsc` during dev — type errors will not block the dev server. Run `tsc --noEmit` manually before committing to catch them.

### Infrastructure

```bash
podman-compose up -d postgres redis minio
psql -h localhost -U lims -d lims -f lims-web/src/main/resources/db/schema.sql
# dev profile DB password: lims_dev_password (application-dev.yml)
```

## Conventions

### Database
- UUID primary keys (`String` in Java), no physical foreign keys (enforced in app layer)
- Audit columns on every table: `created_at/updated_at/created_by/updated_by`
- Logical delete via `deleted_at` (NULL = alive, `NOW()` = deleted) — MyBatis-Plus is configured for this; **never write physical `DELETE` in Mappers**
- Optimistic lock via `version` column
- snake_case table/column names; camelCase Java fields — mapping configured globally in `application.yml`

### API
- All endpoints under prefix `/api/v1` — keep it that way
- Controllers return `R<T>` (`{code, message, data, timestamp}`); paged results use `PageResult<T>` (`records / total / size / current`)
- Validation failures throw `BusinessException(ErrorCode.PARAM_VALIDATION_FAILED)`
- Update `docs/runbook/api-summary.md` (table of endpoints + role matrix) when adding or changing endpoints

### Auth
- Production: `JwtAuthenticationFilter` parses `Authorization: Bearer <JWT>`
- Dev profile: `DevAuthFilter` reads `X-Dev-User` header to simulate a user — use this to bypass Azure AD when debugging the frontend
- Azure AD flow: `/auth/azure-ad/url` → `/auth/azure-ad/callback` (code → JWT)

### External integrations
- **MinIO** (`storage` sub-package): falls back to `${java.io.tmpdir}/lims-files/` when not reachable
- **Word reports**: poi-tl renders template → MinIO → Microsoft Graph creates SharePoint copy → frontend embeds via `/reports/{id}/edit-url`
- **Parts / Suppliers** APIs: wrapped with Resilience4j `@CircuitBreaker(name="partService")` / `@TimeLimiter`; set `EXTERNAL_API_MOCK_ENABLED=true` (default in dev) to return inline mocks
- **Audit**: `@AuditLog` annotation on service methods → AOP writes to `audit_log` table

### Workflow (Flowable)
- Process: `requestProcess` in `lims-workflow/src/main/resources/processes/request-process.bpmn20.xml`
- `businessKey = requestId` on every process instance
- Candidate groups: `ROLE_MANAGER / ROLE_TECHNICIAN / ROLE_ENGINEER`
- State machine: Start → Manager Assign → (assign→Sample Receive / reject→End) → Create Report → Approve Report → (approve→End / reject→Create Report)
- Service facade: `WorkflowService.{startProcess, completeTask, getPendingTasks, getCurrentTask, isProcessCompleted}`

## End-to-End Request Flow (highest-traffic code path)

Modifying this flow? Update **all** of these together:

1. `RequestController` / `RequestService` (in `lims-web`, `lims-service`)
2. `request-process.bpmn20.xml` (in `lims-workflow`)
3. `docs/runbook/api-summary.md` (endpoint + role matrix)
4. `lims-web-ui/src/services/requestService.ts` and `pages/request/RequestDetail/index.tsx`

Sequence: create request (DRAFT) → start process → submit → Manager `/assign` (must include `assignments: [{taskId, engineerId}]` or `ASSIGNMENT_REQUIRED`) → Technician `/receive-sample` (must include `deliveryNote`) → `/start-reporting` → Engineer creates report (poi-tl → MinIO → Graph) → Engineer `/submit` → Manager `/approve` (reject loops back) → Manager `/complete`. Every state change writes `sys_operation_log`; dashboards aggregate from `request` / `analysis_task` / `equipment_repair`.

## Ports & Config

| Service | Port | Notes |
|---|---|---|
| Backend (Spring Boot) | 8080 | API at `/api/v1`, Swagger at `/swagger-ui.html` |
| Frontend dev server | 8000 | Proxies `/api` → backend |
| MinIO API | 9000 | Console on 9001 |
| PostgreSQL | 5432 | |
| Redis | 6379 | |

Configuration is environment-variable driven (`${DB_HOST:localhost}` style) — never hardcode production values in `application*.yml`. Only `application-dev.yml` may carry localhost defaults.

## Don't

- Don't bump Spring Boot, Flowable, MyBatis-Plus, or Java major versions — `pom.xml` pins them
- Don't write physical `DELETE` in Mappers — logical delete is configured
- Don't commit `.env`, Azure AD secrets, JWT secret, or MinIO credentials (already in `.gitignore`)
- Don't add an endpoint without updating `docs/runbook/api-summary.md` and the role matrix
- Don't use the Java package layout inside a module as the source of truth for what the module does — see the table above

## Additional documentation in `docs/`

- `docs/design/material-lims-design.md` — full design doc (ER, fields, business rules)
- `docs/runbook/deployment.md` — operations / runbook
- `docs/runbook/api-summary.md` — REST endpoint catalog + role matrix
- `docs/runbook/user-manual.md` — end-user manual
- `docs/writing/` — project briefs, phase plans, phase summaries
