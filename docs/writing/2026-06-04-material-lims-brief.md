# Material LIMS 项目简报 (Brainstorm Brief)

## 项目定位

材料实验室信息管理系统（Material LIMS），服务于材料实验的委托、分析、报告全流程管理。核心用户约50人以内，部署于企业内网，已具备 Microsoft 365 企业订阅。

---

## 一、核心业务流程

```
委托创建 → Manager分配/修正/退回 → Technician样品接收与制样 → Engineer创建报告(Word在线编辑) → Manager审批报告 → 报告完成
                                                                    ↑
                                                            延期需选择原因
                                                            Revise Report 可发起版本升级
```

流程关键节点间需自动计时，用于仪表盘展示及权限分配。

---

## 二、模块全景图

### A. 基础数据层（6个配置表单）
Brand、Request Type、Holidays、Request Notes、Department、Knowledge Hub —— 这些是系统的"字典数据"，为后续业务表单提供选择数据源。

### B. 测试数据层（5个配置表单）
Test Group、Test Site、Analysis Type、Analysis Items、Specification —— 构成测试知识体系，Analysis Items 是最复杂的配置项，包含设备、标准、成本、单价、单位等多维信息。

### C. 核心业务层
1. **Request 委托单** —— 流程表单，系统最核心模块
2. **历史版本报告存档** —— 版本管理
3. **Request 看板** —— 仪表盘
4. **Request 成本统计看板** —— 仪表盘

### D. 设备管理层
1. Equipment List —— 设备台账
2. Equipment Status —— 状态仪表盘
3. Equipment Repair —— 维修记录（有打印模板，支持导出）

### E. 集成与配置层
1. 组织架构/人员信息同步（Teams/AD）
2. 零部件实时查询接口
3. 供应商实时查询接口
4. 节假日工作日计算插件
5. 报告模板与在线编辑（必须项，M365）
6. 单点登录（OAuth2.0/SAML）
7. 中英文双语界面
8. 日志管理
9. 个人工作台与Manager全局视图

---

## 三、技术栈推荐方案

### 推荐方案：Spring Boot + React + PostgreSQL

| 层级 | 技术选型 | 理由 |
|------|----------|------|
| 前端 | React 18 + TypeScript + Ant Design Pro | 企业级组件丰富，表单/表格/工作流组件成熟；ProLayout/ProTable 适合后台系统；社区活跃 |
| 后端 | Java 17 + Spring Boot 3.x | 企业标准，安全框架成熟（Spring Security），工作流集成好（Flowable），与 Microsoft Graph API 对接生态完善 |
| 数据库 | PostgreSQL 15+ | 企业级可靠性，JSONB 支持灵活字段，全文搜索，免费开源 |
| 工作流 | Flowable 7.x | 轻量可嵌入，支持 BPMN 2.0，与 Spring Boot 原生集成，适合审批流场景 |
| Word在线编辑 | Microsoft 365 Online (Graph API) | 已有订阅，所见即所得编辑体验最佳，无需额外授权 |
| SSO | Azure AD + OAuth2.0 / SAML | 已有 M365 订阅意味着已有 Azure AD，天然集成 |
| 缓存 | Redis | 会话管理、节假日缓存、仪表盘数据缓存 |
| 文件存储 | MinIO | 内网对象存储，兼容 S3 协议，存储报告文件和附件 |
| PDF转换 | LibreOffice HEADLESS + Apache POI | 内网部署，Word→PDF 转换，无需外部服务 |

### 备选方案对比

| 方案 | 优势 | 劣势 | 适用场景 |
|------|------|------|----------|
| Python FastAPI + Vue | 开发速度快，Python生态好 | 工作流引擎选择少，企业安全框架不如Java成熟 | 原型验证、小团队 |
| Node.js + React | JS全栈，前后端统一 | 工作流/企业安全生态弱 | 轻量级应用 |
| 低代码平台(如Appian) | 快速交付 | 定制性差，M365集成受限，成本高 | 标准化流程 |

---

## 四、关键架构决策点

### 1. 工作流引擎选型：Flowable vs 自研轻量流程

需求中的流程是相对固定的4步审批流（创建→分配→制样→报告→审批），但存在退回、拒绝、版本升级等分支。

- **Flowable 方案**：标准化，支持流程定义可视化，未来可扩展，但学习曲线较高
- **自研状态机方案**：简单直接，但扩展性差

**建议**：采用 Flowable，因为需求中存在多条分支路径（退回、拒绝、Revise Report），且未来可能增加节点

### 2. Word 在线编辑集成方案

已有 M365 企业订阅，推荐两阶段实现：

**阶段一（MVP）**：利用 Microsoft Graph API + SharePoint/OneDrive 实现文档协作
- 创建报告时，后端通过 Graph API 在 SharePoint 创建 Word 文档
- 前端嵌入 Microsoft 365 Online 编辑器（WOPI 协议或 iframe 嵌入）
- 编辑完成后回写元数据

**阶段二（增强）**：报告模板引擎
- 基于 Apache POI + Word 模板（docx4j 或 poi-tl）动态生成报告骨架
- 自动填充委托信息、Part信息、分析项等
- 工程师在 M365 Online 中编辑正文和结论

### 3. 报告版本管理策略

```
Request No: REQ-2026-001
├── V1.0 (初始报告，审批通过) → Revise Report Archive
├── V1.1 (修改报告，审批通过) → Revise Report Archive  
└── V2.0 (当前最新版本) → All Request 中展示
```

**关键设计**：
- 版本号采用 Major.Minor 格式
- 每次审批通过后若再修改，版本号递增
- 修改原因（Revision Note）作为必填字段写入报告
- 最新版在 Request 中展示，历史版在 Archive 中存档
- 文件命名：`{RequestNo}_V{版本号}_{日期}.docx`

### 4. 外部系统集成架构

```
┌─────────────────────────────────────────────────┐
│                   LIMS 系统                       │
│                                                   │
│  ┌─────────┐  ┌─────────┐  ┌──────────────┐     │
│  │ 同步服务 │  │ 查询服务 │  │  认证服务    │     │
│  │(定时任务)│  │(实时API) │  │ (SSO/OAuth)  │     │
│  └────┬────┘  └────┬────┘  └──────┬───────┘     │
└───────┼────────────┼──────────────┼──────────────┘
        │            │              │
   ┌────▼────┐  ┌───▼────┐   ┌────▼────┐
   │Teams/AD │  │零部件   │   │Azure AD │
   │Graph API│  │主数据API│   │OAuth2.0 │
   └─────────┘  └────────┘   └─────────┘
                     │
              ┌─────▼─────┐
              │供应商管理  │
              │系统API     │
              └────────────┘
```

- **人员/部门同步**：定时任务（如每小时），增量同步，需去重逻辑
- **零部件/供应商查询**：实时按需查询，不存储全量数据，仅缓存查询结果用于关联
- **SSO**：Azure AD 天然与 M365 联动

### 5. 权限模型设计

基于角色的权限控制（RBAC），4个核心角色：

| 角色 | 权限范围 |
|------|----------|
| Requester（委托人） | 创建委托、查看自己的委托、下载报告 |
| Manager | 分配委托、修正信息、审批报告、查看全局数据、成本统计 |
| Technician | 样品接收、制样状态更新 |
| Engineer | 创建报告、编辑报告、查看分配给自己的任务 |
| Admin | 系统配置、基础数据维护、日志查看 |

**特殊场景**：
- 代下单：实验室人员代替委托人下单，需记录真实委托人
- 一个用户可能兼具多个角色（如 Engineer + Manager）

### 6. 仪表盘与看板设计

**Request 看板（工程师视图）**：
- 待处理 / 进行中 / 待审批 / 已完成 —— Kanban 风格
- 临期提醒（黄色预警 / 红色超期）
- 支持筛选（按Brand、Request Type、优先级等）

**Request 看板（Manager视图）**：
- 全局请求概览
- 分配状态统计
- 审批待办列表
- 人员工作量分布

**成本统计看板**：
- 按 Brand / Request Type / 时间维度统计
- Analysis Item 成本明细
- 图表联动（点击柱状图过滤表格数据）

**设备状态仪表盘**：
- 在用 / 维修中 / 停用 —— 饼图
- 设备利用率趋势

---

## 五、数据模型核心实体

```
Brand ──┐
        ├── Request ──┬── AnalysisTask ──┬── Report (V1, V2, ...)
Department ─┘         │                  │
                      ├── Sample         └── ReportRevision
                      │
RequestType ──────────┤
                      │
PartInfo (API查询) ───┤
Supplier (API查询) ───┘

AnalysisItem ── Equipment ── EquipmentRepair
     │
AnalysisType ── TestGroup
     │
TestSite ── Specification
```

---

## 六、高风险项与缓解策略

| 风险项 | 风险等级 | 描述 | 缓解策略 |
|--------|----------|------|----------|
| Word在线编辑集成 | **高** | M365 Graph API 集成复杂度高，WOPI协议需要公网或特殊网络配置 | 1. 先验证 WOPI 在内网部署的可行性 2. 备选：iframe 嵌入 SharePoint Online 编辑页 3. 最终备选：OnlyOffice 自部署 |
| 外部API依赖 | **中** | 零部件/供应商主数据API可能不稳定或不可用 | 1. API调用增加熔断/降级机制 2. 支持手动录入兜底 3. 本地缓存最近查询结果 |
| 工作日计算 | **中** | 不同地区节假日规则不同 | 1. 每年人工维护节假日表 2. 提供导入模板 3. 支持自定义工作日规则 |
| 报告版本管理 | **中** | 并发修改可能导致版本冲突 | 1. 乐观锁控制 2. 编辑时锁定文档 3. 版本号自动递增 |
| 双语支持 | **低** | 字段、菜单、说明文本的中英文维护 | 1. i18n 资源文件统一管理 2. 前端 i18next / 后端 MessageSource |

---

## 七、开发阶段建议

### Phase 1: 基础框架与核心数据（4-6周）
- 项目脚手架搭建（Spring Boot + React）
- 数据库设计与基础CRUD
- SSO集成（Azure AD OAuth2.0）
- 基础数据管理（Brand、Department、Test Group等6个配置模块）
- 权限模型实现

### Phase 2: 核心流程（6-8周）
- Request 委托单流程（Flowable集成）
- 样品接收与制样
- 人员/部门同步（Teams/AD）
- Request 看板

### Phase 3: 报告系统（6-8周）
- 报告模板生成（Apache POI + poi-tl）
- M365 Online 在线编辑集成
- 报告审批流程
- Word/PDF 下载
- 报告版本管理与归档

### Phase 4: 扩展功能（4-6周）
- 零部件/供应商API集成
- 成本统计看板
- 设备管理模块
- 设备状态仪表盘
- 节假日计算插件
- 双语支持完善

### Phase 5: 优化与上线（2-4周）
- 日志管理
- Knowledge Hub
- 性能优化
- UAT测试与修复
- 知识库文档编写

**总预估：22-32周（约5.5-8个月）**

---

## 八、待确认事项

1. 零部件/供应商主数据系统的API文档是否已就绪？鉴权方式？
2. 是否需要移动端适配（如平板查看看板）？
3. 报告是否有标准模板格式？还是需要系统支持自定义模板？
4. 成本统计是否需要导出功能（Excel等）？
5. 除了Request流程外，是否还有其他审批流程需求？
6. 数据备份和灾难恢复策略是否有要求？
