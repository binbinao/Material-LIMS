# Material LIMS 用户操作手册

适用版本：1.0.0。

## 一、登录与角色

系统支持企业 Azure AD SSO 单点登录，访问 `/login` 后跳转微软认证页。账号属性同步自 AD/Teams，`ADMIN` 角色由系统管理员在「系统管理 → 用户管理」分配。

| 角色 | 主要场景 |
|------|---------|
| Requester | 填写委托、跟踪自己的委托、下载报告 |
| Manager | 分配委托、审批报告、查看全局看板和成本统计 |
| Technician | 接收样品、维护制样状态、登记设备维修 |
| Engineer | 创建/编辑/提交报告 |
| Admin | 基础数据、节假日、用户角色、操作日志、i18n、知识库 |

## 二、Requester 操作

委托管理 → 创建委托：选择 Brand、Request Type 后系统按 Request Type 的「任务时长（工作日）」自动计算 Due Date（自动跳过周末和节假日）。提交后进入 Manager 待审。

委托列表中可以看到自己的所有委托。状态变成 `COMPLETED` 后即可在详情页下载 Word/PDF 报告。

## 三、Manager 操作

委托管理 → 委托列表：可见全局所有委托。在 `PENDING` 状态下点击「Assign」分配工程师；点击「Reject」退回需要填理由。

委托完成进入 `REPORT_REVIEW` 状态后，进「报告管理 → 报告列表」点 Approve/Reject。Reject 会回退到 Engineer 修改重交。

仪表盘：
- Request 看板：状态分布 + 临期红黄预警
- 成本统计：支持按 Brand / Request Type / 月份 / Analysis Item 维度切换，可导出 Excel
- 设备状态：Active / Under Repair / Decommissioned 三色饼图

## 四、Technician 操作

委托管理 → 详情页 → Receive Sample：填写到货单号后委托进入 `RECEIVING_SAMPLE`，再点 Start Reporting 移交工程师。

设备管理 → 维修记录：
1. 点 Report Repair，选设备 + 故障描述 → 提交后系统自动把该设备置为 `UNDER_REPAIR`
2. 维修完成后点列表行的 Complete，填写处置措施和成本 → 系统自动把设备恢复 `ACTIVE`（若该设备没有其他未完成维修单）

## 五、Engineer 操作

工作台 → 我的待办：列出分配给自己的 Analysis Task。

报告管理 → 我的报告 → 点「Edit」进入 M365 Online 在线编辑（首次使用浏览器需登录 Azure AD），编辑完成后点 Sync 把内容拉回服务器。如需版本升级（已审批的报告再修改），点 Revise 填写 Revision Note，新建 V1.1 / V2.0。

委托详情 → 任务列表：可标记 IN_PROGRESS / DELAYED（DELAYED 必填延期原因）/ COMPLETED。

## 六、Admin 操作

基础数据：维护 Brand / Request Type / Department / Holiday / Request Note；下拉数据均带 30 分钟缓存，编辑后自动失效。

测试数据：维护 Test Group / Test Site / Analysis Type / Analysis Items / Specification。Analysis Item 是最复杂配置项，含 Equipment 关联、Standard、Cost、Unit Price、Unit。

节假日：每年 12 月用 CSV 模板批量导入下一年节假日，支持 NATIONAL（全员放假）和 COMPANY（公司加班/补班）两类。

知识库：上传 Manual（PDF/DOCX）或 Video，全体登录用户可下载。

操作日志：所有写操作（创建/更新/删除/审批/上传）自动留痕，含调用者 IP、请求载荷 JSON、用户名联表展示。点 View 看完整 payload。

国际化：维护 zh-CN / en-US 双语字典，前端启动时拉取覆盖默认值。新增的 key 立即对所有用户生效（缓存自动失效）。

## 七、通用快捷键 / 行为

- 表格列：右上角设置图标可调列宽、显隐、固定列
- 搜索框：高级筛选支持时间区间
- 翻页：URL 自带 page/size 参数，刷新不丢上下文
- 切换语言：右上角语言菜单（zh-CN / en-US）

## 八、问题求助

- 系统报错截图 + 时间 + 当前 URL → 提交 IT 工单
- 业务流程问题 → 找直属 Manager
- 数据异常 → Admin 在「操作日志」按时间反查变更人
