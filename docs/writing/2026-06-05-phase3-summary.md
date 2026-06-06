# Material-LIMS Phase 3 完成总结

> 完成时间：2026-06-05  
> 验证：`mvn -DskipTests clean compile` BUILD SUCCESS（8 模块全绿，零警告）  
> 计划文档：`docs/writing/2026-06-05-phase3-plan.md`

## 一、交付物清单

### P0 安全与认证

| 类 | 路径 | 作用 |
|---|---|---|
| `JwtTokenProvider` | `lims-common/security` | Hutool HS256 签发/校验 LIMS 自签 JWT，TTL 8h |
| `SecurityUtils` | `lims-common/security` | 从 SecurityContext 取当前用户 |
| `JwtAuthenticationFilter` | `lims-web/security` | Bearer + LIMS_TOKEN cookie 双通道解析 |
| `DevAuthFilter` | `lims-web/security` | dev profile 注入虚拟 ADMIN 用户 |
| `SecurityConfig` | `lims-web/config` | dev permitAll + DevAuthFilter；prod JWT + 角色路径匹配 |
| `AuthService.handleCallback` | `lims-service` | 真实 Azure AD OAuth Code Flow，写 httpOnly cookie |

### P1 Microsoft Graph

| 类 | 作用 |
|---|---|
| `MicrosoftGraphClient` | msal4j Client Credentials Flow + RestTemplate REST，token 缓存 60s 续期，`@odata.nextLink` 分页处理 |
| `AzureAdSyncService` | 用 `ObjectProvider` 优雅注入；按 `external_id` fallback `email` upsert User；按 `external_id` upsert Department |

### P2 报告生成与 PDF

| 类 | 作用 |
|---|---|
| `FileStorageService` | MinIO 8.5.9 + 本地 `/tmp/lims-files` 兜底，7 天预签名 URL |
| `WordToPdfConverter` | LibreOffice headless `--convert-to pdf`，超时可配，`failOnError=false` 优雅降级 |
| `ReportTemplateService` | poi-tl 1.12.2 模板渲染；模板缺失自动生成纯文本占位 docx |
| `ReportService.createReport / reviseReport` | 串联 generate → upload docx → convert pdf → upload pdf → 写库 |

### P4 数据权限与定时任务

| 类 | 作用 |
|---|---|
| `DataPermissionInterceptor` | jsqlparser 4.9 解析 SELECT，按角色注入 `requester_id / assignee_id / author_id` 条件，ADMIN/MANAGER 跳过 |
| `MybatisPlusConfig` | DataPermission → Pagination(POSTGRE_SQL) → OptimisticLocker 三件套 |
| `OverdueAlertScheduler` | 每小时 cron 扫描，RED/ORANGE/YELLOW/GREEN 四级告警（当前打日志，留消息推送占位） |

## 二、按计划延后的内容

P3 SharePoint 集成：依赖真实 Azure AD 与 SharePoint 站点可达环境，dev 环境无法验证，按原计划延后到具备生产环境时再补。

## 三、可优雅降级的开关

| 配置项 | 默认 | 关闭时行为 |
|---|---|---|
| `azure.ad.enabled` | `false` | Graph 客户端不实例化，AzureAdSyncService 跳过同步；OAuth 回调返回 mock 用户 |
| `minio.enabled` | `false` | 文件落到 `${java.io.tmpdir}/lims-files/`，返回 `file://` URI |
| `libreoffice.fail-on-error` | `false` | 找不到 libreoffice 命令时 PDF 转换跳过，仅保留 docx |
| Spring profile = `dev` | 是 | DevAuthFilter 注入虚拟 ADMIN，permitAll 全部接口；prod 才启用 JWT |

意味着 Phase 3 落地后，dev 环境**无需**搭建 Azure AD / MinIO / LibreOffice 也能完整启动并跑通业务流；prod 环境只需提供 4 个环境变量：`AZURE_AD_TENANT_ID / CLIENT_ID / CLIENT_SECRET / JWT_SECRET`。

## 四、构建环境修复

本次顺手修复了项目原有 `.mvn/wrapper/maven-wrapper.jar` 损坏的问题：用 `mvn wrapper:wrapper -Dmaven=3.9.16` 重新生成为 only-script 模式（不再依赖本地 jar，运行时自动下载 Maven 发行包），现在 `./mvnw -version` 工作正常。

## 五、剩余技术债（已记录，下一轮处理）

1. `lims-service/src/main/resources/templates/report_template.docx` 实体模板文件未放入仓库——当前运行时缺失会自动降级为纯文本占位 docx，不阻塞流程，但建议尽快补一份带变量占位符的设计模板
2. `ExternalApiService` 的 baseUrl 仍未配置（Phase 2 遗留，与 Phase 3 无关）
3. `AuditLogAspect` 的 `detail` JSON 字段尚未序列化方法入参/返回值（已可工作，仅信息密度可提升）

## 六、验证命令

```bash
cd /Users/duobinji/Documents/GitHub/Material-LIMS
./mvnw -DskipTests clean compile      # 全模块编译，期望 BUILD SUCCESS
./mvnw -DskipTests -pl lims-web -am package   # 打包可执行 jar
java -jar lims-web/target/lims-web-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```
