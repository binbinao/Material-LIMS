# ADR-001: Maven 7-Module Layered Architecture

## Status
Accepted (2026-06-04)

## Context
Material-LIMS is a Spring Boot 3.2 monolith that needs clear separation between web (Controller), business logic (Service), data access (DAO), domain model (Model), common utilities (Common), workflow engine (Workflow), and admin management (Admin).

## Decision
Adopt a 7-module Maven multi-module structure with strict dependency direction:

```
lims-web (入口) → lims-service → lims-dao → lims-model → lims-common
                 ↘ lims-workflow → lims-common
                 ↘ lims-admin    → lims-dao
```

### Module Responsibilities

| Module | Layer | Key Contents |
|--------|-------|-------------|
| `lims-common` | Foundation | JwtTokenProvider, BusinessException, ErrorCode, SecurityUtils |
| `lims-model` | Domain | Entity, DTO, VO, Enum (RequestStatus, ReportStatus, RoleEnum) |
| `lims-dao` | Data Access | MyBatis-Plus Mapper interfaces |
| `lims-workflow` | Workflow | Flowable BPMN integration, WorkflowService |
| `lims-service` | Business Logic | RequestService, ReportService, FileStorageService |
| `lims-admin` | Admin | System management services |
| `lims-web` | Presentation | Controllers, SecurityConfig, Spring Boot entry point |
| `lims-web-ui` | Frontend | React 18 + Ant Design Pro (separate npm project) |

### Dependency Rules
- `lims-common` has zero internal dependencies (leaf module)
- `lims-model` depends only on `lims-common`
- `lims-dao` depends on `lims-model` + `lims-common`
- `lims-workflow` depends only on `lims-common` (decoupled from business domain)
- `lims-service` depends on `lims-dao` + `lims-model` + `lims-workflow` + `lims-common`
- `lims-web` depends on `lims-service` + `lims-workflow` + `lims-admin` + `lims-common`

## Consequences

### Positive
- Clear compile-time dependency enforcement (no circular dependencies possible)
- Each module independently testable
- workflow module decoupled from business domain, enabling future replacement
- New developers can understand the system by reading module structure alone

### Negative
- 7 modules = 7 pom.xml files to maintain version alignment
- Cross-module refactoring requires touching multiple modules
- `lims-admin` module currently thin — may justify merging into `lims-service` if admin features remain light

### Alternatives Considered
- **Single module monolith**: Simpler but loses compile-time boundary enforcement
- **Microservices**: Over-engineered for a lab management system with ~20 tables; adds deployment complexity without business benefit
- **5 modules (merge admin+workflow into service)**: Viable if workflow logic stays simple; current 7-module split chosen for explicit boundary clarity

## Date
2026-06-04