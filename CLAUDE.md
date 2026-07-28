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

## ECC Skills — Mandatory Project Rules 🛡️

> **核心原则：做任何变更前，必须先检查以下 ECC Skill 是否适用于当前场景。如果能用，必须严格遵循。**

这些 ECC Skill 是经过项目上下文评估后选定的强制规范，非可选建议。违反这些规则等同于违反项目编码规范。

### 变更前必查清单（按变更类型）

在做以下类型的变更时，**必须**在动手前加载对应的 ECC Skill：

| 变更类型 | 必用 Skill | 触发条件 | 作用 |
|----------|-----------|----------|------|
| **后端代码（任何 Java 文件）** | `springboot-patterns` | 编写/修改 Controller, Service, Mapper, Config | 架构模式、分层规范、命名约定 |
| **后端功能开发 / Bug 修复** | `springboot-tdd` | 新增功能、修复 bug、重构 | JUnit 5 + Mockito + Testcontainers TDD |
| **后端 PR / 发布前** | `springboot-verification` | 提交 PR、准备发布 | 构建 + 静态分析 + 测试覆盖率 + 安全检查 |
| **认证 / 鉴权 / 安全相关** | `springboot-security` + `security-review` | 修改 auth、权限、用户输入、密钥 | 认证鉴权最佳实践 + 全面安全检查表 |
| **前端组件 / 页面开发** | `react-patterns` + `react-testing` | 编写/修改 React 组件、hooks、页面 | 组件模式 + RTL/Vitest 测试 |
| **前端性能优化** | `react-performance` | 优化渲染、减小包体积、改进加载速度 | 70+ 条性能规则，分 8 个优先级 |
| **数据库 Schema / 查询** | `postgres-patterns` | 修改 schema.sql、写 SQL、加索引 | PG 查询优化、索引策略、Schema 设计 |
| **E2E 测试** | `e2e-testing` + `webapp-testing` | 编写/修改 Playwright 测试 | Page Object Model + 浏览器交互验证 |
| **代码健康检查** | `codehealth-mcp` | 重构前检查、提交前审查 | CodeScene 代码健康扫描 |
| **任何新功能开发** | `tdd-workflow` | 从零开发新功能 | 80%+ 覆盖率 TDD：单元 + 集成 + E2E |

### 执行流程

```
变更请求
  │
  ├─ 1. 识别变更类型（后端/前端/数据库/安全/E2E）
  ├─ 2. 查上表 → 确定必用 Skill
  ├─ 3. use_skill("<skill-name>") 加载 Skill
  ├─ 4. 严格遵循 Skill 中的规范和模式
  └─ 5. 变更完成后运行验证（springboot-verification / react-testing）
```

### 示例

```
# 要修改 RequestService.java
→ 变更类型：后端代码
→ 必用 Skill：springboot-patterns, springboot-tdd
→ 先 use_skill("springboot-patterns") 确认架构模式
→ 再 use_skill("springboot-tdd") 遵循 TDD 流程写测试
→ 最后 use_skill("springboot-verification") 验证通过

# 要优化前端列表页渲染性能
→ 变更类型：前端性能优化
→ 必用 Skill：react-performance, react-patterns
→ 先 use_skill("react-performance") 获取 70+ 规则
→ 再 use_skill("react-patterns") 确保组件模式正确

# 要给数据库加新表
→ 变更类型：数据库 Schema
→ 必用 Skill：postgres-patterns
→ 先 use_skill("postgres-patterns") 确认索引和查询设计
→ 注意：本项目无物理外键，记得用逻辑外键！
```

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
