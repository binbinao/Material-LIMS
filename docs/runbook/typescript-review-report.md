# TypeScript代码审查报告

> 🤖 由 `scripts/typescript-reviewer.mjs` 自动生成  
> 📅 审查时间: 2026/6/24 22:56:32  
> 🔧 模式: 严格模式

## 📊 审查统计

| 层级 | ⛔ ERROR | ⚠️ WARN | 💡 INFO |
|------|----------|---------|---------|
| type | 0 | 23 | 101 |
| service | 0 | 0 | 0 |
| component | 0 | 0 | 0 |
| utility | 0 | 0 | 0 |
| **总计** | **0** | **23** | **101** |

## 🔍 发现问题详情

### TYPE层

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `historyIntelli.ts`  
  ↳ 匹配: `interface UmiPath...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `historyIntelli.ts`  
  ↳ 匹配: `interface UmiHistory...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `pluginConfig.ts`  
  ↳ 匹配: `checkDepCssModules?: boolean;...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `interface AccessProps...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `Exception.tsx`  
  ↳ 匹配: `route?: IRoute;...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `SelectLang.tsx`  
  ↳ 匹配: `interface HeaderDropdownProps...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `SelectLang.tsx`  
  ↳ 匹配: `interface LocalData...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `SelectLang.tsx`  
  ↳ 匹配: `interface SelectLangProps...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `SelectLang.tsx`  
  ↳ 匹配: `globalIconClassName?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `SelectLang.tsx`  
  ↳ 匹配: `className?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `SelectLang.tsx`  
  ↳ 匹配: `reload?: boolean;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `runtimeConfig.d.ts`  
  ↳ 匹配: `cache?: IntlCache;...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `interface ExecutorProps...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `request.ts`  
  ↳ 匹配: `interface RequestConfig...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `request.ts`  
  ↳ 匹配: `data?: T;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `request.ts`  
  ↳ 匹配: `skipErrorHandler?: boolean;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `request.ts`  
  ↳ 匹配: `errorHandler?: IErrorHandler;...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `historyIntelli.ts`  
  ↳ 匹配: `interface UmiPath...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `pluginConfig.ts`  
  ↳ 匹配: `checkDepCssModules?: boolean;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `Exception.tsx`  
  ↳ 匹配: `route?: IRoute;...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `SelectLang.tsx`  
  ↳ 匹配: `interface HeaderDropdownProps...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `SelectLang.tsx`  
  ↳ 匹配: `interface LocalData...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `SelectLang.tsx`  
  ↳ 匹配: `globalIconClassName?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `SelectLang.tsx`  
  ↳ 匹配: `className?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `SelectLang.tsx`  
  ↳ 匹配: `reload?: boolean;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `runtimeConfig.d.ts`  
  ↳ 匹配: `cache?: IntlCache;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `request.ts`  
  ↳ 匹配: `data?: T;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `request.ts`  
  ↳ 匹配: `skipErrorHandler?: boolean;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `request.ts`  
  ↳ 匹配: `errorHandler?: IErrorHandler;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `zhCN?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `enUS?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `level?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `externalId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `sortOrder?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `requestService.ts`  
  ↳ 匹配: `page?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `crudPageButtons.tsx`  
  ↳ 匹配: `editOpensModalTitle?: string;...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface CurrentUser...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface Brand...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface RequestCreateDT...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface Request...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface AnalysisTask...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface Report...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface RequestType...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface Department...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface Holiday...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface Equipment...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface AnalysisItem...`

- **⚠️ WARN**: 接口建议使用I前缀命名约定  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `interface SysUser...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `deptId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `id?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `description?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `sortOrder?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `deptId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `partNumber?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `partName?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `eco?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `supplierCode?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `supplierName?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `priority?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `proxyRequest?: boolean;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `realRequesterName?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `deptId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `proxyRequesterId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `realRequesterName?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `partNumber?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `partName?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `eco?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `supplierCode?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `supplierName?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `requestReason?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `dueDate?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `sampleDeliveryNote?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `totalCost?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `processInstanceId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `submittedAt?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `assignedAt?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `assigneeId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `delayReason?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `startedAt?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `completedAt?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `sortOrder?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `taskId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `revisionNote?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `fileUrl?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `pdfUrl?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `sharepointFileId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `sharepointEditUrl?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `approvedBy?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `approvedAt?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `submittedAt?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `id?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `taskDurationDays?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `description?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `active?: boolean;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `id?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `parentId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `sortOrder?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `id?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `id?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `model?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `serialNumber?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `location?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `purchaseDate?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `warrantyExpiry?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `description?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `id?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `groupId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `testSite?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `analysisType?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `cost?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `durationDays?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `specificationId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `active?: boolean;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `loginId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `deptId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `externalId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `isActive?: boolean;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `typings.d.ts`  
  ↳ 匹配: `lastLoginAt?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `requestService.ts`  
  ↳ 匹配: `page?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `zhCN?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `enUS?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `level?: number;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `externalId?: string;...`

- **💡 INFO**: 可选属性建议使用明确的null或undefined类型  
  ↳ 文件: `index.tsx`  
  ↳ 匹配: `sortOrder?: number;...`

## 🔧 自动修复统计

✅ 成功修复 23 个问题:

- **historyIntelli.ts**: 接口建议使用I前缀命名约定
- **historyIntelli.ts**: 接口建议使用I前缀命名约定
- **index.tsx**: 接口建议使用I前缀命名约定
- **SelectLang.tsx**: 接口建议使用I前缀命名约定
- **SelectLang.tsx**: 接口建议使用I前缀命名约定
- **SelectLang.tsx**: 接口建议使用I前缀命名约定
- **index.tsx**: 接口建议使用I前缀命名约定
- **request.ts**: 接口建议使用I前缀命名约定
- **historyIntelli.ts**: 接口建议使用I前缀命名约定
- **SelectLang.tsx**: 接口建议使用I前缀命名约定
- **SelectLang.tsx**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
- **typings.d.ts**: 接口建议使用I前缀命名约定
