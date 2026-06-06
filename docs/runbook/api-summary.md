# Material LIMS REST API 摘要

基础前缀：`/api/v1`。
认证：`Authorization: Bearer <JWT>`，从 `/auth/azure-ad/callback` 获取。
返回包：`{code: 200, message, data}`，分页用 `Page<T>` 含 `records / total / size / current`。
开发模式 Swagger UI：`/swagger-ui.html`。

## 一、认证 Auth

| Method | 路径 | 角色 | 说明 |
|--------|------|------|------|
| GET | `/auth/azure-ad/url` | 公开 | 取 Azure AD 登录跳转地址 |
| POST | `/auth/azure-ad/callback` | 公开 | 用 code 换 JWT |
| GET | `/auth/me` | 已登录 | 当前用户信息 |
| POST | `/auth/logout` | 已登录 | 注销 |
| PUT | `/auth/me/locale` | 已登录 | 设置个人 locale |

## 二、基础数据

| Method | 路径 | 角色 |
|--------|------|------|
| GET / POST | `/brands` | 列表公开 / 写 ADMIN |
| PUT / DELETE | `/brands/{id}` | ADMIN |
| GET / POST | `/request-types` | 列表公开 / 写 ADMIN |
| PUT / DELETE | `/request-types/{id}` | ADMIN |
| GET / POST | `/departments` | 列表公开 / 写 ADMIN |
| GET | `/departments/tree` | 公开 |
| PUT / DELETE | `/departments/{id}` | ADMIN |
| GET / POST | `/holidays` | 列表公开 / 写 ADMIN |
| POST | `/holidays/import` | ADMIN |
| GET | `/holidays/calculate-due-date?startDate=&days=` | 已登录 |
| GET | `/holidays/is-business-day?date=` | 已登录 |
| GET | `/holidays/count-business-days?startDate=&endDate=` | 已登录 |

## 三、测试数据

| Method | 路径 | 角色 |
|--------|------|------|
| GET / POST / PUT / DELETE | `/analysis-items` | 列表公开 / 写 ADMIN |
| GET | `/analysis-items/cascade` | 公开（含缓存） |

## 四、委托与任务

| Method | 路径 | 角色 |
|--------|------|------|
| GET / POST | `/requests` | 已登录 |
| GET | `/requests/{id}` | 数据权限过滤 |
| POST | `/requests/{id}/submit` | Requester |
| POST | `/requests/{id}/assign` | MANAGER |
| POST | `/requests/{id}/reject` | MANAGER |
| POST | `/requests/{id}/receive-sample` | TECHNICIAN |
| POST | `/requests/{id}/start-reporting` | TECHNICIAN |
| POST | `/requests/{id}/complete` | MANAGER |
| GET | `/requests/{id}/tasks` | 已登录 |
| PUT | `/requests/tasks/{taskId}` | ENGINEER |
| GET | `/requests/{id}/workflow` | 已登录 |
| GET | `/requests/my-tasks` | 已登录 |

## 五、报告

| Method | 路径 | 角色 |
|--------|------|------|
| GET / POST | `/reports/requests/{requestId}/reports` | ENGINEER 创建 |
| GET | `/reports/{id}` | 数据权限 |
| POST | `/reports/{id}/submit` | ENGINEER |
| POST | `/reports/{id}/approve` | MANAGER |
| POST | `/reports/{id}/reject` | MANAGER |
| POST | `/reports/{id}/revise` | ENGINEER |
| GET | `/reports/{id}/revisions` | 已登录 |
| GET | `/reports/{id}/edit-url` | ENGINEER |
| POST | `/reports/{id}/sync` | ENGINEER |

## 六、设备与维修

| Method | 路径 | 角色 |
|--------|------|------|
| GET / POST / PUT / DELETE | `/equipments` | 列表公开 / 写 ADMIN |
| GET / POST | `/equipment-repairs` | 已登录 / TECHNICIAN+ |
| PUT | `/equipment-repairs/{id}` | TECHNICIAN+ |
| POST | `/equipment-repairs/{id}/complete` | TECHNICIAN+ |
| DELETE | `/equipment-repairs/{id}` | ADMIN |

## 七、外部数据

| Method | 路径 | 角色 |
|--------|------|------|
| GET | `/external/parts?keyword=` | 已登录 |
| GET | `/external/parts/{id}` | 已登录 |
| GET | `/external/suppliers?keyword=` | 已登录 |
| GET | `/external/suppliers/{id}` | 已登录 |

## 八、仪表盘

| Method | 路径 | 角色 |
|--------|------|------|
| GET | `/dashboard/my-tasks?userId=` | 已登录 |
| GET | `/dashboard/manager-overview` | MANAGER |
| GET | `/dashboard/request-stats` | MANAGER |
| GET | `/dashboard/cost-stats?startDate=&endDate=&groupBy=` | MANAGER |
| GET | `/dashboard/cost-export?...` | MANAGER（返回 xlsx） |
| GET | `/dashboard/equipment-stats` | 已登录 |

## 九、知识库

| Method | 路径 | 角色 |
|--------|------|------|
| GET | `/knowledge-docs` | 已登录 |
| GET | `/knowledge-docs/{id}` | 已登录 |
| POST | `/knowledge-docs` (multipart) | MANAGER+ |
| PUT | `/knowledge-docs/{id}` | MANAGER+ |
| DELETE | `/knowledge-docs/{id}` | ADMIN |

## 十、系统管理

| Method | 路径 | 角色 |
|--------|------|------|
| GET | `/admin/users` | ADMIN |
| PUT | `/admin/users/{id}/roles` | ADMIN |
| PUT | `/admin/users/{id}/toggle-active` | ADMIN |
| GET | `/admin/logs?module=&action=&userId=&startDate=&endDate=` | ADMIN |
| GET | `/admin/logs/{id}` | ADMIN |
| GET | `/i18n/messages?locale=` | 公开 |
| POST | `/i18n/messages` | ADMIN |
| POST | `/i18n/messages/batch?locale=` | ADMIN |
| DELETE | `/i18n/messages?messageKey=&locale=` | ADMIN |
| POST | `/sync/users` | ADMIN |
| POST | `/sync/departments` | ADMIN |

## 十一、错误码摘要

| code | 含义 |
|------|------|
| 1001 | 参数校验失败 |
| 1002 | 数据不存在 |
| 1003 | 数据已存在 |
| 1004 | 操作不允许 |
| 2001 | Request 状态不允许此操作 |
| 2002 | Report 版本冲突 |
| 2003 | 已超期 |
| 2004 | Report 当前状态不可编辑 |
| 2005 | 必须分配工程师 |
| 2006 | Revision Note 必填 |
| 3001 / 3002 / 3003 | 未授权 / 拒绝访问 / Token 过期 |
| 5001 | 外部 API 不可用（自动降级 mock） |
| 5002 | 文件转换失败 |
| 5003 | M365 集成错误 |
| 5004 | 文件上传失败 |
