# Testing Infrastructure — Sub-project A

> Status: Draft
> Date: 2026-06-06
> Author: brainstorming session
> Sub-project of: "Comprehensive testing (unit + integration + user)"

## 1. Goal

Establish a working test foundation for Material LIMS that future sub-projects (B–I: business unit tests, controller IT, BPMN IT, frontend tests, E2E, UAT, security, external-failure) can build on, **without writing any business test logic in this sub-project**. Deliver a smoke-test GitHub Actions workflow proving the foundation runs end-to-end on a clean clone.

Out of scope: any real test of business code, coverage-threshold enforcement, pre-commit hooks, snapshot policy, mutation testing, performance testing, Docker image build, deployment.

## 2. Background

The repository currently contains zero tests: no `src/test/java` or `src/test/typescript` directories, no `*Test.java` or `*.test.ts` files, no Testcontainers / MockMvc / Jest / Vitest configuration. Only `lims-web/pom.xml` declares `spring-boot-starter-test`; the other 6 modules have no test dependencies. `package.json` exposes `npm test` invoking `jest` with no Jest installation or config.

The system is a multi-module Spring Boot 3.2 / React 18 / Umi Max 4 / Ant Design Pro 6 / Flowable 7 / PostgreSQL 15 / Redis 7 / MinIO application. Production authentication flows through Azure AD OAuth2; reports round-trip through Microsoft 365 Graph; external parts/suppliers APIs are called with Resilience4j circuit breakers.

## 3. Decisions

| # | Decision | Choice |
|---|----------|--------|
| D1 | Backend unit-test DB | H2 in-memory, PG-flavored schema adapter |
| D2 | Backend integration-test DB | Testcontainers PostgreSQL 15 |
| D3 | Test-physical separation | Maven Failsafe + `*IT.java` naming |
| D4 | Test-logical separation | JUnit 5 `@Tag("integration")` (excluded from surefire) |
| D5 | Frontend test runner | Vitest + jsdom |
| D6 | Frontend assertions | React Testing Library + `@testing-library/jest-dom` |
| D7 | Frontend API mocking | MSW (Mock Service Worker) |
| D8 | Modules wired | `lims-common`, `lims-dao`, `lims-service`, `lims-workflow`, `lims-admin`, `lims-web` (skip `lims-model`) |
| D9 | CI surface | GitHub Actions, two jobs (backend, frontend), smoke only |
| D10 | Mock infrastructure | Reusable `TestXxxConfig` classes in `lims-web` |
| D11 | Coverage | JaCoCo report at `verify` phase, no threshold, no failure |

Rationale for combined D3+D4: surefire/failsafe enforces CI behavior (which Maven phase runs what); `@Tag` enforces developer/IDE behavior (which tests appear in the run). Using only one creates a gap; using both is cheap.

Rationale for D10: the system has four external dependencies (Azure AD, MinIO, Microsoft Graph, parts/suppliers APIs) plus Flowable. Each requires a test-time replacement. Centralising these in named `@TestConfiguration` classes inside `lims-web/src/test/java/com/lims/web/config/` gives every future test a zero-config opt-in (`@Import(TestSecurityConfig.class)`).

## 4. File-Level Plan

### 4.1 Root build

**`pom.xml`** (modify)
- Add `<dependencyManagement>` entries for:
  - `org.junit.jupiter:junit-jupiter:5.10.2`
  - `org.assertj:assertj-core:3.25.3`
  - `org.mockito:mockito-junit-jupiter:5.11.0`
  - `org.testcontainers:testcontainers-bom:1.19.7` (BOM import)
  - `org.testcontainers:postgresql` (version from BOM)
  - `org.testcontainers:junit-jupiter` (version from BOM)
  - `org.springframework.security:spring-security-test` (managed by Spring Boot BOM)
  - `org.wiremock:wiremock-standalone:3.5.4`
  - `org.jacoco:jacoco-maven-plugin:0.8.11` (plugin only)
- Add `<build><plugins>` entries for:
  - `maven-surefire-plugin` — `excludedGroups=integration`, JUnit 5 engine
  - `maven-failsafe-plugin` — `includes=**/*IT.java`, JUnit 5 engine
  - `jacoco-maven-plugin` — `prepare-agent` on `initialize`, `report` on `verify`

### 4.2 Backend modules

For each of the 6 modules, modify `pom.xml` to add the test dependencies below; add `src/test/java` and (where required) `src/test/resources`. Each module's `*Test.java` examples use only Mockito + AssertJ (no Spring context) so they stay fast.

| Module | Added test deps | Example file |
|--------|-----------------|--------------|
| `lims-common` | `spring-boot-starter-test`, `assertj-core`, `junit-jupiter` | `com/lims/common/util/HolidayCalendarTest.java` (≥3 cases: weekday in week, weekend, holiday, holiday-on-weekend) |
| `lims-dao` | `spring-boot-starter-test`, `com.h2database:h2` (test), `testcontainers-postgresql`, `testcontainers-junit-jupiter` | `AbstractDaoIT.java` (Testcontainers PG base with `@Container static PostgreSQLContainer`) + `BrandMapperIT.java` (≥1 insert/select case) |
| `lims-service` | `spring-boot-starter-test`, `assertj-core`, `mockito-junit-jupiter` | `BrandServiceTest.java` (≥2 cases: list + create with mocked Mapper) + `ReportServiceTest.java` (≥1 case) |
| `lims-workflow` | `spring-boot-starter-test`, `testcontainers-postgresql` | `WorkflowServiceTest.java` (mocked RuntimeService/TaskService, ≥2 cases) + `WorkflowServiceIT.java` (real Flowable engine on Testcontainers, ≥1 case starting `requestProcess`) |
| `lims-admin` | `spring-boot-starter-test` | `UserServiceTest.java` (≥1 case) |
| `lims-web` | `spring-boot-starter-test`, `spring-security-test`, `wiremock-standalone` | see 4.3 |

### 4.3 `lims-web` test infrastructure

`lims-web/src/test/java/com/lims/web/`:

- `AbstractIntegrationTest.java` — annotated `@SpringBootTest(webEnvironment = RANDOM_PORT)`, `@ActiveProfiles("test")`, `@Tag("integration")`, `@AutoConfigureMockMvc`, starts a `PostgreSQLContainer` via Testcontainers; provides a `@BeforeAll` that exposes the JDBC URL to Spring via `@DynamicPropertySource`. Subclasses inherit MockMvc + JWT helpers.
- `HealthControllerTest.java` — `*Test.java` (unit-tier): one `MockMvc` test that asserts a public health endpoint (or a future `/actuator/health`) returns 200. Proves the security + MockMvc wiring.
- `config/TestSecurityConfig.java` — `@TestConfiguration` with a `@Bean` of type `JwtDecoder` returning a no-op decoder. Annotated classes use `SecurityMockMvcRequestPostProcessors.jwt().jwt(j -> j.claim("roles", "ADMIN").claim("sub", "u-1"))` to inject roles.
- `config/TestMinioConfig.java` — `@TestConfiguration @Profile("test")` providing `@Bean @Primary MinioClient` returning a mock; tests can `@TempDir` a local file folder to satisfy file-storage calls.
- `config/TestGraphConfig.java` — `@TestConfiguration` with `@RegisterExtension WireMockExtension` pointed at `http://localhost:0`; supplies a `GraphServiceClient` bean whose base URL is rewritten to the WireMock port.
- `config/TestExternalApiConfig.java` — `@TestConfiguration` providing `@MockBean` substitutes for `PartsApiClient` and `SuppliersApiClient`; default return values via `Mockito.lenient().when(...)`.

`lims-web/src/test/resources/application-test.yml`:
- `spring.datasource.url=${TEST_PG_URL:jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL}` (Testcontainers overrides at runtime via `@DynamicPropertySource`)
- **Do not set `spring.datasource.driver-class-name`** — let Spring Boot auto-detect from the JDBC URL prefix; setting H2 driver explicitly would break Testcontainers PG runs
- `flowable.database-schema-update=true`
- `flowable.async-executor-activate=false` (test determinism)
- `azure.ad.*=placeholder` (consumed by `TestSecurityConfig`)
- `minio.endpoint=http://localhost:0` (consumed by `TestMinioConfig`)
- `logging.level.org.flowable=WARN`

`lims-dao/src/test/resources/schema-h2.sql` (file exists but is not consumed by any test in this sub-project):
- Translate `schema.sql` (PostgreSQL 15) to H2-compatible form:
  - `TIMESTAMP DEFAULT NOW()` → `TIMESTAMP DEFAULT CURRENT_TIMESTAMP`
  - Drop `CHECK (type IN (...))` in favour of simple `VARCHAR(20)`
  - Strip `UUID` casts (H2 has no UUID type without extension; substitute `VARCHAR(36)`)
  - Replace `BOOLEAN DEFAULT TRUE` with `BOOLEAN DEFAULT TRUE` (H2 supports this)
  - Document remaining divergences in a header comment
- **Note**: No `*Test.java` in this sub-project uses H2. This file is staged for sub-project B (business unit tests) which may use `@DataJpaTest` with H2. Adding the file now means the schema is reviewable once, not twice.

### 4.4 Frontend

`lims-web-ui/package.json` — add to `devDependencies`:
- `vitest@^1.6.0`
- `@vitest/ui@^1.6.0`
- `@testing-library/react@^16.0.0`
- `@testing-library/jest-dom@^6.4.0`
- `@testing-library/user-event@^14.5.0`
- `@testing-library/dom@^10.0.0`
- `jsdom@^24.0.0` (primary DOM environment per D5)
- `msw@^2.2.0`
- `happy-dom@^14.0.0` (do NOT install in this sub-project; listed here as the documented fallback if jsdom + ProLayout portals surface issues — install only when the symptom appears)

Add to `scripts`:
- `"test": "vitest"`
- `"test:run": "vitest run"`
- `"test:ui": "vitest --ui"`
- `"test:coverage": "vitest run --coverage"`
- Keep `"start": "npm run dev"` unchanged.

`lims-web-ui/vitest.config.ts` (new):
- `test.environment: 'jsdom'`
- `test.setupFiles: ['./src/test/setup.ts']`
- `resolve.alias`: mirror `tsconfig.json` paths (`@/*` → `src/*`)
- `test.css: false` (Ant Design CSS imports become identity-obj-proxy)
- Exclude `node_modules`, `.umi`, `dist`

`lims-web-ui/src/test/setup.ts` (new):
- `import '@testing-library/jest-dom/vitest'`
- `import { server } from './server'`; `beforeAll(() => server.listen())`; `afterEach(() => server.resetHandlers())`; `afterAll(() => server.close())`

`lims-web-ui/src/test/server.ts` (new): `setupServer(...handlers)` from MSW node.

`lims-web-ui/src/test/handlers.ts` (new): default MSW handlers for `GET /api/v1/auth/me`, `GET /api/v1/brands` returning fixture JSON. Tests can `server.use(...)` to override per case.

`lims-web-ui/src/test/renderWithProviders.tsx` (new): `render(ui, { currentUser }) => render(<ConfigProvider><AccessProvider initialState={...}>{ui}</AccessProvider></ConfigProvider>)`. Wraps Ant Design `ConfigProvider` and the minimal Umi `useAccess`/initial-state context.

`lims-web-ui/src/test/factories.ts` (new): `brandFactory()`, `requestFactory()`, `userFactory()` returning deterministic objects; sequence-based IDs to keep snapshots stable.

`lims-web-ui/src/access.test.ts` (new): ≥3 cases — `canAdmin` true for ADMIN; false for ENGINEER; `canManager` true for MANAGER.

`lims-web-ui/src/app.test.tsx` (new): ≥1 case — `getInitialState` returns `{ currentUser }` when `/auth/me` responds with the fixture.

`lims-web-ui/src/services/requestService.test.ts` (new): ≥2 cases — `getBrands` MSW-handled; `getRequests` MSW-handled with query params.

### 4.5 CI

`.github/workflows/test.yml` (new):
- `name: tests`
- `on: pull_request, push (branches: [main])`
- `jobs.backend-tests` (ubuntu-latest):
  - `actions/checkout@v4`
  - `actions/setup-java@v4` with `distribution: temurin`, `java-version: 17`, `cache: maven`
  - `mvn -B verify` (runs unit + integration with Testcontainers; uses host Docker)
- `jobs.frontend-tests` (ubuntu-latest):
  - `actions/checkout@v4`
  - `actions/setup-node@v4` with `node-version: 20`, `cache: npm`, `cache-dependency-path: lims-web-ui/package-lock.json`
  - `cd lims-web-ui && npm ci && npm run test:run`

No secrets, no coverage upload, no artifact, no Docker image build.

### 4.6 Documentation

- `docs/testing/README.md` (new):
  - "How to run tests locally": `./mvnw test`, `./mvnw verify`, `cd lims-web-ui && npm test`
  - Naming/tag conventions: `*Test.java` (surefire, H2 or no-DB), `*IT.java` + `@Tag("integration")` (failsafe, Testcontainers)
  - Testcontainers requirement: Docker must be running; first run downloads the PG image
  - When to use `@SpringBootTest` vs `@DataJpaTest` vs Mockito
  - Frontend Vitest commands
  - Link to `mock-strategy.md`

- `docs/testing/mock-strategy.md` (new):
  - Table of `TestSecurityConfig` / `TestMinioConfig` / `TestGraphConfig` / `TestExternalApiConfig` with what they replace and how to use them
  - Example: `@SpringBootTest @Import(TestSecurityConfig.class)` with `mockMvc.perform(get("/api/v1/admin/users").with(jwt().jwt(j -> j.claim("roles", "ADMIN"))))`
  - Note on `application-test.yml` overrides

- `CODEBUDDY.md` (modify):
  - Append a "Testing" section documenting the new test commands and structure
  - Update the "常用命令" section with backend test commands and frontend test commands
  - **Update the existing `前端测试` entry**: replace `npm test # jest` with `npm test # vitest`, and align with the scripts added in 4.4 (`test` / `test:run` / `test:ui` / `test:coverage`)

## 5. Acceptance Criteria

1. `./mvnw test` runs in <60s on a clean clone (no Docker required for this phase).
2. `./mvnw verify` runs in <10 min on a clean clone with Docker available; all `*IT.java` pass.
3. `cd lims-web-ui && npm run test:run` exits 0 with all example tests passing.
4. `git push` to a branch with the workflow file triggers `backend-tests` and `frontend-tests` jobs in GitHub Actions; both green.
5. JaCoCo HTML report appears at each module's `target/site/jacoco/index.html`; opening it in a browser shows the example classes.
6. Adding a new `*Test.java` in any wired module picks up the right dependencies automatically.
7. Adding a new `*IT.java` triggers Testcontainers PG startup automatically; Failsafe picks it up.
8. `docs/testing/README.md` and `docs/testing/mock-strategy.md` exist and link from `CODEBUDDY.md`.

## 6. Risk Register

| Risk | Mitigation |
|------|-----------|
| Testcontainers-in-GH-Actions startup >5 min | Use `docker/setup-buildx-action@v3`; PG image is small; the example IT count is ≤1 to keep CI fast |
| H2 ↔ PG SQL drift accumulates | `schema-h2.sql` is the only H2 schema; divergences are documented in its header; DAO ITs run against real PG only |
| Vitest + Umi Max jest ecosystem conflict | Umi internal jest runner untouched; `vitest.config.ts` is a separate file with its own config; tests live in `src/test/` outside Umi's conventions |
| `TestMinioConfig` race condition with shared local FS | `@TempDir` per test class; no shared file paths |
| `lims-web` `@SpringBootTest` slow startup | Use `webEnvironment = RANDOM_PORT` + `MockMvc`; example tests don't depend on Tomcat |
| WireMock 3.x + Spring Boot 3.2 API drift | Pin `wiremock-standalone:3.5.4`; sample usage in `TestGraphConfig` |
| Frontend RTL + ProLayout SSR/portal issues | `renderWithProviders` wraps with `<ConfigProvider>`; if ProLayout portals fail under jsdom, fall back to `@testing-library/react`'s `baseElement` override or use `happy-dom` |

## 7. Out of Scope (Deferred)

- Business unit tests for `RequestService` / `ReportService` / `WorkflowService` / `AuthService` → sub-project B
- Controller API integration tests → sub-project C
- BPMN flow end-to-end tests → sub-project D
- Frontend component / page tests → sub-project E
- Playwright E2E user scenarios → sub-project F
- UAT manual scripts → sub-project G
- Security / role / data-scope tests → sub-project H
- External integration failure-mode tests → sub-project I
- Coverage threshold enforcement (JaCoCo `check` goal with minimum ratio) → sub-project J (new)
- Pre-commit hooks via Husky → sub-project K (new)
- Mutation testing via PIT → sub-project L (new)

## 8. Open Questions

None. All architectural decisions resolved during brainstorming. The only implementation-time choices (e.g., test data factory contents, exact WireMock stub payloads) are delegated to the implementation plan.

## 9. References

- `pom.xml` (root and modules)
- `lims-web/src/main/resources/application.yml`, `application-dev.yml`
- `lims-web/src/main/resources/db/schema.sql`, `init.sql`
- `docs/design/material-lims-design.md` §1.4 (工程结构)
- `docs/runbook/api-summary.md` (API surface used by example tests)
- `CODEBUDDY.md` (updated by this sub-project)
