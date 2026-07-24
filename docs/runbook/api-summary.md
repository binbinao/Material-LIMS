# API 接口速查表

> 🤖 此文件由 `scripts/doc-updater.mjs` 自动生成
> 📅 最后更新：2026-07-24
> 📦 扫描目录：`lims-web/src/main/java/com/lims/web/controller/`

## 1. Admin

**Base**: `/api/v1/admin`

> 系统管理

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/admin/logs` | List audit logs with filters and user-name join |
| **GET** | `/api/v1/admin/logs/{id}` | Get audit log detail by id |
| **GET** | `/api/v1/admin/users` | List users with pagination |
| **PUT** | `/api/v1/admin/users/{id}/roles` | Update user roles |
| **PUT** | `/api/v1/admin/users/{id}/toggle-active` | Toggle user active status |

## 2. Analysis Item Management

**Base**: `/api/v1/analysis-items`

> 分析项目管理

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/analysis-items` | Get / |
| **GET** | `/api/v1/analysis-items/{id}` | Get cascade data for frontend selection |
| **GET** | `/api/v1/analysis-items/by-group/{groupId}` | Get /by-group/{groupId} |
| **GET** | `/api/v1/analysis-items/cascade` | Get /cascade |
| **POST** | `/api/v1/analysis-items` | Post / |
| **PUT** | `/api/v1/analysis-items/{id}` | Put /{id} |
| **DEL** | `/api/v1/analysis-items/{id}` | Delete /{id} |

## 3. Authentication

**Base**: `/api/v1/auth`

> SSO认证

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/auth/azure-ad-login` | Get /azure-ad-login |
| **GET** | `/api/v1/auth/azure-ad/url` | Get /azure-ad/url |
| **GET** | `/api/v1/auth/me` | Get /me |
| **POST** | `/api/v1/auth/azure-ad/callback` | Post /azure-ad/callback |
| **POST** | `/api/v1/auth/callback` | Post /callback |
| **POST** | `/api/v1/auth/login` | Post /login |
| **POST** | `/api/v1/auth/logout` | Post /logout |
| **PUT** | `/api/v1/auth/me/locale` | Put /me/locale |
| **PUT** | `/api/v1/auth/password` | Put /password |

## 4. Brand Management

**Base**: `/api/v1/brands`

> 品牌管理

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/brands` | List brands with pagination |
| **GET** | `/api/v1/brands/{id}` | Get brand by ID |
| **POST** | `/api/v1/brands` | Create brand |
| **PUT** | `/api/v1/brands/{id}` | Update brand |
| **DEL** | `/api/v1/brands/{id}` | Delete brand |

## 5. Dashboard

**Base**: `/api/v1/dashboard`

> 仪表盘

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/dashboard/cost-export` | Export cost statistics to xlsx |
| **GET** | `/api/v1/dashboard/cost-stats` | Cost statistics with multiple grouping dimensions: brand|type|month|item |
| **GET** | `/api/v1/dashboard/equipment-stats` | Get /equipment-stats |
| **GET** | `/api/v1/dashboard/manager-overview` | Get /manager-overview |
| **GET** | `/api/v1/dashboard/my-tasks` | Get /my-tasks |
| **GET** | `/api/v1/dashboard/request-stats` | Get /request-stats |

## 6. Department Management

**Base**: `/api/v1/departments`

> 部门管理

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/departments` | Get / |
| **GET** | `/api/v1/departments/{id}` | Get department tree structure |
| **GET** | `/api/v1/departments/tree` | Get /tree |
| **POST** | `/api/v1/departments` | Post / |
| **PUT** | `/api/v1/departments/{id}` | Put /{id} |
| **DEL** | `/api/v1/departments/{id}` | Delete /{id} |

## 7. Equipment Calibration

**Base**: `/api/v1/equipment-calibrations`

> 设备校准管理

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/equipment-calibrations/equipment/{equipmentId}` | Get calibration history for equipment |
| **GET** | `/api/v1/equipment-calibrations/equipment/{equipmentId}/valid` | Check if equipment calibration is valid |
| **GET** | `/api/v1/equipment-calibrations/expiring` | Get calibrations expiring soon |
| **POST** | `/api/v1/equipment-calibrations` | Create calibration record |

## 8. Equipment Repair

**Base**: `/api/v1/equipment-repairs`

> 设备维修管理

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/equipment-repairs` | Get / |
| **GET** | `/api/v1/equipment-repairs/{id}` | Get /{id} |
| **POST** | `/api/v1/equipment-repairs` | Create repair record (auto sets equipment.status=UNDER_REPAIR) |
| **POST** | `/api/v1/equipment-repairs/{id}/complete` | Mark repair completed; equipment auto restored to ACTIVE if no more pending repairs |
| **PUT** | `/api/v1/equipment-repairs/{id}` | Put /{id} |
| **DEL** | `/api/v1/equipment-repairs/{id}` | Delete /{id} |

## 9. Equipment Management

**Base**: `/api/v1/equipments`

> 设备管理

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/equipments` | Get / |
| **GET** | `/api/v1/equipments/{id}` | Get /{id} |
| **POST** | `/api/v1/equipments` | Post / |
| **PUT** | `/api/v1/equipments/{id}` | Put /{id} |
| **DEL** | `/api/v1/equipments/{id}` | Delete /{id} |
| **PATCH** | `/api/v1/equipments/{id}/status` | Patch /{id}/status |

## 10. External Integration

**Base**: `/api/v1/external`

> 外部系统集成

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/external/parts` | Search parts from master data system |
| **GET** | `/api/v1/external/parts/{partNumber}` | Get part detail by partNumber |
| **GET** | `/api/v1/external/suppliers` | Search suppliers from supplier management system |
| **GET** | `/api/v1/external/suppliers/{supplierCode}` | Get supplier detail by supplierCode |

## 11. Holiday Management

**Base**: `/api/v1/holidays`

> 节假日管理 / 工作日计算

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/holidays` | Get / |
| **GET** | `/api/v1/holidays/{id}` | Get /{id} |
| **GET** | `/api/v1/holidays/calculate-due-date` | Compute due date by skipping holidays/weekends |
| **GET** | `/api/v1/holidays/count-business-days` | Count business days between [from, to] (inclusive) |
| **GET** | `/api/v1/holidays/is-business-day` | Check whether the given date is a business day |
| **POST** | `/api/v1/holidays` | Post / |
| **POST** | `/api/v1/holidays/import` | Post /import |
| **PUT** | `/api/v1/holidays/{id}` | Put /{id} |
| **DEL** | `/api/v1/holidays/{id}` | Delete /{id} |

## 12. i18n

**Base**: `/api/v1/i18n`

> 国际化字典

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/i18n/messages` | Get /messages |
| **POST** | `/api/v1/i18n/messages` | Post /messages |
| **POST** | `/api/v1/i18n/messages/batch` | Post /messages/batch |
| **DEL** | `/api/v1/i18n/messages` | Delete /messages |

## 13. Knowledge Hub

**Base**: `/api/v1/knowledge-docs`

> 知识库文档

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/knowledge-docs` | Get / |
| **GET** | `/api/v1/knowledge-docs/{id}` | Get /{id} |
| **POST** | `/api/v1/knowledge-docs` | Upload knowledge document (multipart) |
| **PUT** | `/api/v1/knowledge-docs/{id}` | Put /{id} |
| **DEL** | `/api/v1/knowledge-docs/{id}` | Delete /{id} |

## 14. Report Management

**Base**: `/api/v1/reports`

> 报告管理

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/reports` | Get / |
| **GET** | `/api/v1/reports/{id}` | Get /{id} |
| **GET** | `/api/v1/reports/{id}/edit-url` | Get /{id}/edit-url |
| **GET** | `/api/v1/reports/{id}/revisions` | Get /{id}/revisions |
| **GET** | `/api/v1/reports/{id}/sample-word` | Get /{id}/sample-word |
| **POST** | `/api/v1/reports/{id}/approve` | Post /{id}/approve |
| **POST** | `/api/v1/reports/{id}/reject` | Post /{id}/reject |
| **POST** | `/api/v1/reports/{id}/revise` | Post /{id}/revise |
| **POST** | `/api/v1/reports/{id}/submit` | Post /{id}/submit |
| **POST** | `/api/v1/reports/{id}/sync` | Post /{id}/sync |
| **POST** | `/api/v1/reports/requests/{requestId}/reports` | Post /requests/{requestId}/reports |

## 15. Request Type Management

**Base**: `/api/v1/request-types`

> 委托类型管理

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/request-types` | Get / |
| **GET** | `/api/v1/request-types/{id}` | Get /{id} |
| **POST** | `/api/v1/request-types` | Post / |
| **PUT** | `/api/v1/request-types/{id}` | Put /{id} |
| **DEL** | `/api/v1/request-types/{id}` | Delete /{id} |

## 16. Request Management

**Base**: `/api/v1/requests`

> 委托管理

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/requests` | List requests with pagination and filters |
| **GET** | `/api/v1/requests/{id}` | Get request by ID |
| **GET** | `/api/v1/requests/{id}/tasks` | Get analysis tasks for a request |
| **GET** | `/api/v1/requests/{id}/workflow` | Get workflow status for a request |
| **GET** | `/api/v1/requests/my-tasks` | Get my pending workflow tasks |
| **POST** | `/api/v1/requests` | Create a new request |
| **POST** | `/api/v1/requests/{id}/advance-to-approving` | Manager advances request to approval phase |
| **POST** | `/api/v1/requests/{id}/assign` | Manager assigns engineers to request |
| **POST** | `/api/v1/requests/{id}/complete` | Complete request |
| **POST** | `/api/v1/requests/{id}/receive-sample` | Receive sample for request |
| **POST** | `/api/v1/requests/{id}/reject` | Manager rejects request |
| **POST** | `/api/v1/requests/{id}/start-reporting` | Start reporting phase |
| **POST** | `/api/v1/requests/{id}/submit` | Submit request for review |
| **PUT** | `/api/v1/requests/tasks/{taskId}` | Update analysis task status |

## 17. Data Sync

**Base**: `/api/v1/sync`

> 数据同步

| 方法 | 路径 | 说明 |
|------|------|------|
| **GET** | `/api/v1/sync/status` | Get /status |
| **POST** | `/api/v1/sync/departments` | Post /departments |
| **POST** | `/api/v1/sync/users` | Post /users |

---

📊 **统计**: 17 个 Controller · 109 个端点
