# ADR-003: Dual-Profile Security Strategy (dev/prod)

## Status
Accepted (2026-06-04)

## Context
开发环境需要快速迭代和测试，生产环境需要严格的 JWT + RBAC 认证。需要一套安全策略既满足开发便利性，又保证生产安全性，且不因 profile 切换导致授权注解（`@PreAuthorize`）行为不一致。

## Decision
采用 **Spring @Profile 双 FilterChain** 策略：

### Dev Profile (`spring.profiles.active=dev`)
```
JwtAuthenticationFilter → DevAuthFilter → Controller
```
- **JwtAuthenticationFilter**：优先解析 `LIMS_TOKEN` Cookie 或 `Authorization: Bearer` Header
- **DevAuthFilter**：fallback 策略，如果 JWT filter 未设置 context，从 `X-Dev-User` header 读取角色并注入虚拟 Principal
- **所有请求 permitAll()**，鉴权完全依赖 Controller 上的 `@PreAuthorize`
- `@EnableMethodSecurity` 在 dev 和 prod 下均启用，确保授权注解行为一致

### Prod Profile (`spring.profiles.active=prod`)
```
JwtAuthenticationFilter → Controller
```
- 仅 JWT 认证（自签 token）
- 严格 RBAC：`/api/v1/admin/**` → `hasRole('ADMIN')`，其他路径 `denyAll()`
- 无状态 Session（`SessionCreationPolicy.STATELESS`）

### Dev User Mapping (X-Dev-User Header)
| Header Value | Real User ID | Role |
|-------------|-------------|------|
| `admin` | `user-admin-001` | ADMIN |
| `manager` | `user-manager-001` | MANAGER |
| `engineer` | `user-engineer-001` | ENGINEER |
| `tech` | `user-tech-001` | TECHNICIAN |
| `requester` | `user-requester-001` | REQUESTER |
| (default) | `dev-user-0001` | ADMIN,MANAGER,ENGINEER,REQUESTER,TECHNICIAN |

## Consequences

### Positive
- 开发体验顺畅：`curl -H "X-Dev-User: engineer" localhost:8080/api/v1/requests` 即可测试
- `@PreAuthorize` 注解在 dev 和 prod 下行为完全一致（`@EnableMethodSecurity` 不受 profile 影响）
- 生产环境安全边界清晰：仅 `/auth/**`、`/swagger-ui/**`、`/actuator/health` 放行
- 无 dev profile 泄漏到生产环境的风险（`@Profile("dev")` 注解保证 DevAuthFilter 在 prod 下不加载）

### Negative
- 双 FilterChain 需要维护两套规则，新增 endpoint 时需同步更新 prod 配置
- `DataPermissionInterceptor` 在 dev profile 下被禁用（避免干扰数据级测试），团队需单独验证数据权限
- prod 模式下 self-signed JWT 不支持 token 撤销

### Alternatives Considered
- **Spring Security OAuth2 Resource Server**：适合对接 Azure AD，但增加外部依赖。当前留给 `lims-common` 的 `AzureAdJwtDecoder` 作为可选项
- **单一 FilterChain + 环境变量控制**：减少了代码重复，但 `if(dev)` 分支散落在 SecurityConfig 中，不如 `@Profile` 干净
- **API Key**：更简单但缺少用户身份信息（无法支持 `SecurityUtils.getCurrentUserId()`）

## Date
2026-06-04