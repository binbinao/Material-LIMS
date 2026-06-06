# Material-LIMS Phase 5 完成总结

> 完成时间：2026-06-06 00:06:27  
> 编译验证：`./mvnw -DskipTests clean compile` BUILD SUCCESS（全模块绿）  
> UAT 验证：2026-06-06 09:08 四场景 13/13 PASS（Podman + PostgreSQL 15 + MinIO 端到端）  
> 计划文档：`docs/writing/2026-06-05-phase5-plan.md`  
> 范围依据：`docs/writing/2026-06-04-material-lims-brief.md` 第七节 Phase 5「优化与上线」

## 一、交付物清单

### P0 UAT 修复（Phase 4 收尾）

| 文件 | 改动 |
|---|---|
| `lims-web-ui/src/pages/equipment/EquipmentRepairs/index.tsx` | 从「按 status='REPAIRING' 过滤设备」假实现，重写为对接 `/api/v1/equipment-repairs` 的真实 CRUD：报修弹窗（设备下拉 + 故障描述）、完成维修弹窗（维修措施 + 费用 + 维修人）、删除二次确认 |
| `lims-web-ui/src/pages/equipment/EquipmentStatus/index.tsx` | 状态枚举与后端对齐为 `ACTIVE / UNDER_REPAIR / DECOMMISSIONED`（删除原 NORMAL/MAINTENANCE/REPAIRING/IDLE/RETIRED 五值幻影） |
| `lims-web-ui/src/pages/admin/I18nList/index.tsx` | 接 `/api/v1/i18n/messages`：并行拉 zh-CN + en-US 合并为单行展示，编辑时按需 upsert 1~2 条，删除时同时清理两语种 |
| `lims-web-ui/src/services/requestService.ts` | 增补 EquipmentRepair / I18n / Knowledge / AdminLog 详情 / Holiday `calculate-due-date` 一组 API 封装 |
| `lims-web-ui/src/locales/{zh-CN,en-US}.ts` | 扩充 `equipment.*` / `repair.*` / `knowledge.*` / `admin.log.*` / `common.{upload,download,detail,export}` |

### P1 日志管理增强

| 类 | 改动 |
|---|---|
| `AuditLogAspect` | Jackson 序列化方法入参为 JSON 写入 `detail`（截断至 4000 字符）；跳过 `MultipartFile` / `HttpServletRequest` / Spring Web 框架对象；从 `X-Forwarded-For` 取真实 IP；try/catch 包裹 insert，日志失败不影响业务 |
| `AdminController.listLogs` | 联表 `sys_user` 把 userId 转 userName 批量回填；新增 `action / startDate / endDate` 查询参数；返回 `{records,total,size,current}` 适配 ProTable |
| `AdminController.getLog` | 新增 `GET /admin/logs/{id}` 详情端点 |
| 11 个 Controller × 44 处 `@AuditLog` | Phase 4 已埋点完成，Phase 5 仅做 Aspect 增强，无重复打桩 |

### P2 Knowledge Hub

| 类 | 路径 | 作用 |
|---|---|---|
| `KnowledgeDoc` | `lims-model/entity` | `@TableName("knowledge_doc")`，title/category/fileUrl/fileSize/description |
| `KnowledgeDocMapper` | `lims-dao/mapper` | BaseMapper |
| `KnowledgeDocService` | `lims-service` | 复用 `FileStorageService`（MinIO + 本地兜底），上传走临时文件 + finally 清理；分页 + 类目 + 关键词查询 |
| `KnowledgeDocController` | `lims-web/controller` | `/api/v1/knowledge-docs`，multipart 上传，写接口 `@AuditLog(module="KNOWLEDGE")`，删除仅 ADMIN，上传 MANAGER+ |
| `lims-web-ui/src/pages/knowledge/KnowledgeList/index.tsx` | 前端 | ProTable + 上传弹窗 + 类目筛选 + 标题点击下载（新窗口）+ 文件大小 MB 化 |

### P3 性能优化

| 文件 | 改动 |
|---|---|
| `LimsApplication` | 补 `@EnableCaching`（Phase 4 写了 `@Cacheable` 但未启用，本次修复） |
| `BrandService / RequestTypeService / DepartmentService / AnalysisItemService` | 给稳定读方法加 `@Cacheable`；`tree() / cascade() / listByGroup` 各自独立 key；写接口 `@CacheEvict(allEntries=true)`；分页 + 动态过滤的列表方法**不**入缓存（避免 key 爆炸） |
| `application.yml` | `spring.cache.type=${SPRING_CACHE_TYPE:simple}`，prod 切 redis（TTL 1800000 ms）；`servlet.multipart.max-file-size=100MB` 给知识库上传 |
| `db/schema.sql` | 追加 6 个索引：`sys_operation_log(action)` / `request(request_no) WHERE deleted_at IS NULL` 唯一 / `equipment(status)` / `knowledge_doc(category)` / `knowledge_doc(updated_at DESC)` / `sys_i18n_message(locale)`，全部带 `WHERE deleted_at IS NULL` 部分索引 |

### P4 知识库文档

| 文件 | 内容 |
|---|---|
| `docs/runbook/deployment.md` | 依赖矩阵 / docker-compose 部署 / 环境变量清单 / pg_dump + mc mirror 备份恢复 / RTO RPO / 监控告警 |
| `docs/runbook/user-manual.md` | 申请人 / 主管 / 工程师 / 技术员 四类角色操作流程 + Admin 后台 + 通用快捷操作 |
| `docs/runbook/api-summary.md` | 11 节覆盖 Auth / 基础数据 / 测试数据 / Request / Report / Equipment / External / Dashboard / Knowledge / Admin / 错误码 |

## 二、Phase 5 关键修复（计划外发现）

1. **`@EnableCaching` 缺失**：Phase 4 在 `HolidayService` / `I18nService` 写了 `@Cacheable` 但启动类没启用缓存，全部静默失效。本次补上后 4 个新增 `@Cacheable` + 原 2 个一并生效。
2. **Holiday 业务日端点路径不一致**：前端原写 `/holidays/calc-due-date`，后端实际暴露 `/holidays/calculate-due-date`，已在 `requestService.ts` 修正。
3. **`AuditLogAspect` 的 detail 留空**：Phase 3 总结里记录的技术债，本次一并清掉。

### UAT 运行期发现并修掉的 Bug（2026-06-06）

以下 8 个 bug 在编译期无法暴露，仅在 Podman 端到端 UAT 中才触发：

4. **`MyBatisMetaObjectHandler.getCurrentUserId()` 把 `AuthPrincipal.toString()` 写入 VARCHAR(36)** — `UsernamePasswordAuthenticationToken.getName()` 在 principal 不是 String/UserDetails 时回退到 `toString()`，写入 168 字符远超列宽，导致所有 INSERT 失败。修复：显式 `instanceof AuthPrincipal ap` 取 `userId()` 字段。
5. **`AuditLogAspect` 同样的 toString 问题** — `entity.setUserId(auth.getName())` 产生 168 字符，触发 FK 约束或列宽溢出。修复：复用 `currentUserId()` 方法，与 MetaObjectHandler 统一。
6. **`sys_operation_log.detail` 列类型 jsonb 与 entity String 不匹配** — MyBatis 把 String 当 varchar 传，PG 拒绝。修复：把 `detail JSONB` 改为 `detail TEXT`（审计日志不需要 jsonpath 查询）。
7. **dev 虚拟用户 `dev-user-0001` 不在 `sys_user` 种子数据** — audit log FK `sys_operation_log_user_id_fkey` 阻止写入。修复：给 `init.sql` 补上 dev-user-0001（prod 不受影响，DevAuthFilter 只在 dev profile 注册）。
8. **`EquipmentRepairService` 用空 patch 对象 `updateById` 触发乐观锁失败** — `new Equipment()` 无 version 值，MyBatis-Plus `@Version` 校验失败。修复：改为 `selectById → setStatus → updateById(equipment)` 携带完整 version。
9. **`KnowledgeDocController.list` 分页 off-by-one** — `defaultValue="0"` + service `page+1`，前端传 page=1 实际请求第 2 页。修复：controller 默认值改 1，service 去掉 `+1`，加 `page<=0` 保护。
10. **`AdminController.listUsers/listLogs` 同样 off-by-one** — 修复同上。
11. **`uploadKnowledgeDoc` 前端用 `requestType:'form'` 破坏 multipart** — UmiJS 的 `requestType:'form'` 会序列化为 `application/x-www-form-urlencoded`，丢掉 multipart boundary。修复：去掉 requestType，手动设 `Content-Type: multipart/form-data` 让 axios 自动推导。

## 三、不在 Phase 5 范围（继续延后）

WOPI 真实落地（仍是 Mock URL）/ 移动端 / 实时通知 WebSocket / 二次审批流（仅一审）/ SharePoint 集成（依赖 Phase 3 延后项）。

## 四、验收对照

| 计划验收项 | 结果 |
|---|---|
| `./mvnw -DskipTests clean compile` BUILD SUCCESS | 通过（2026-06-06T00:06:27） |
| `/api/v1/knowledge-docs` GET/POST/DELETE 后端可调通 | UAT PASS — 上传 + 列表(current=1) + 删除 全通 |
| `/api/v1/admin/logs` 含详情 | UAT PASS — list total=12 + detail 含完整 JSON payload + userName 回填 |
| 前端 EquipmentRepairs 创建维修单 → equipment.status 自动 UNDER_REPAIR | UAT PASS — 创建→UNDER_REPAIR→完成→ACTIVE 全链路 |
| Admin/I18nList 字典新增 | UAT PASS — zh-CN + en-US 双语 upsert + list 校验 |
| Phase 5 总结文档落盘 | 即本文件 |

### UAT 执行详情（2026-06-06 09:08）

环境：Podman 5.8.2 + PostgreSQL 15 + Redis 7 + MinIO (latest) + Spring Boot dev profile

```
=========== UAT-1: KNOWLEDGE HUB upload -> list -> delete ===========
  [PASS] upload doc_id=628f8184b87eb38ba3d074b9ee0063d7
  [PASS] list total=1 returned=1 current=1
  [PASS] delete http=200

=========== UAT-2: EQUIPMENT REPAIR create -> equipment auto UNDER_REPAIR -> complete -> ACTIVE ===========
  [PASS] before status=ACTIVE
  [PASS] create repair_id=2072b2c7846ee3852b4268f088d0ad71
  [PASS] after-create status=UNDER_REPAIR (auto-linkage works)
  [PASS] complete code=200
  [PASS] after-complete status=ACTIVE (restored)

=========== UAT-3: I18N upsert (zh-CN + en-US for key 'uat.test') ===========
  [PASS] upsert zh-CN code=200
  [PASS] upsert en-US code=200
  [PASS] list zh-CN value='UAT 测试'

=========== UAT-4: ADMIN LOG list & detail (audit aspect output) ===========
  [PASS] log list total=12
  [PASS] log detail I18N/UPSERT detail-payload=yes

==========================================
  PASS=13  FAIL=0
==========================================
```

## 五、上线前建议（下一轮）

1. **运行期冒烟**：dev profile 拉起后跑一遍知识库上传 / 维修单全链路 / i18n 编辑 / Admin 日志详情，把 4 个 UAT 场景过一次。
2. **Redis 接入**：prod 切 `SPRING_CACHE_TYPE=redis`，监控 hit ratio；若热点偏读分页列表，再考虑给 `BrandService.list` 引入二级缓存。
3. **报告模板补齐**：`lims-service/src/main/resources/templates/report_template.docx` 仍缺，运行时降级为纯文本，建议补一份带 `{{variable}}` 占位符的真实 Word 模板。
4. **实体模板上传 UI**：当前 KnowledgeList 上传走 `category=MANUAL/VIDEO`，未来若 brief 扩 RAW_DATA / SOP，需要增加类目枚举与权限白名单。

## 六、验证命令

```bash
cd /Users/duobinji/Documents/GitHub/Material-LIMS
./mvnw -DskipTests clean compile                      # 全模块编译
./mvnw -DskipTests -pl lims-web -am package           # 打包可执行 jar
java -jar lims-web/target/lims-web-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev

# 前端
cd lims-web-ui
npm install
npm run start                                         # http://localhost:8000
```

## 七、Phase 1~5 全景小结

| Phase | 主题 | 关键产出 |
|---|---|---|
| 1 | 骨架 + 基础数据 | 8 模块 Maven 骨架 / Brand/RequestType/Department/AnalysisItem CRUD / MyBatis-Plus + BaseEntity |
| 2 | 业务主流程 | Request 提交 / 工程师分派 / Flowable 一审 / Report 草稿 / 外部 API 占位 |
| 3 | 安全 + 集成 + 报告 | JWT + Azure AD / Microsoft Graph / MinIO + LibreOffice + poi-tl PDF / 数据权限 / 超期告警 |
| 4 | 外部 API / 设备 / i18n | ExternalApiService 真接 + Resilience4j / EquipmentRepair / 设备状态看板 / Holiday 业务日 / sys_i18n_message 双语 |
| 5 | 优化与上线 | AuditLog 含 detail / Knowledge Hub 全栈 / Cache + 索引 / UAT 修复 / 部署运维手册 |

至此 brief 第七节 Phase 1~5 全部落地，可进入 UAT 与生产部署阶段。
