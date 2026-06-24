# TypeScript代码审查指南

> 🤖 Material LIMS TypeScript代码质量保障体系  
> 📅 最后更新: 2024年12月

## 📋 概述

`typescript-reviewer` 是 Material LIMS 项目的 TypeScript 代码质量保障工具，基于四层架构设计，提供智能自动修复和CI/CD强制检查功能。

### 🎯 核心功能

- **四层架构审查**: Component → Service → Type → Utility
- **智能自动修复**: 30+种常见问题一键修复
- **CI强制模式**: ERROR级别问题阻断提交/合并
- **Git集成**: 修复后自动提交
- **详细报告**: 生成Markdown格式审查报告

## 🚀 快速开始

### 安装与配置

```bash
# 确保pre-commit钩子已安装
./scripts/setup-hooks.sh

# 验证安装
node scripts/typescript-reviewer.mjs --help
```

### 基本用法

```bash
# 审查并自动修复
node scripts/typescript-reviewer.mjs

# 预览模式（不实际修复）
node scripts/typescript-reviewer.mjs --dry-run

# CI强制模式（ERROR阻断）
node scripts/typescript-reviewer.mjs --strict

# 只修复不报告
node scripts/typescript-reviewer.mjs --fix-only

# 修复后自动git commit
node scripts/typescript-reviewer.mjs --git-commit
```

## 🏗️ 四层架构审查

### 1. Type层 (类型定义)

**目录**: `lims-web-ui/src/`

**审查重点**:
- 接口命名规范 (`I`前缀)
- 枚举命名规范 (`E`前缀)
- 类型安全性检查
- 可选属性处理

**示例规则**:
```typescript
// ❌ 不符合规范
interface User {
  name?: string;
}

// ✅ 符合规范  
interface IUser {
  name: string | null;
}

enum Status {
  ACTIVE = 'active'
}

// ✅ 符合规范
enum EStatus {
  ACTIVE = 'active'
}
```

### 2. Service层 (API服务)

**目录**: `lims-web-ui/src/services/`

**审查重点**:
- 错误处理完整性
- 函数命名规范 (CRUD前缀)
- API请求标准化
- 业务逻辑封装

**示例规则**:
```typescript
// ❌ 不符合规范
export async function fetchUser(id: string) {
  return request(`/api/users/${id}`);
}

// ✅ 符合规范
export async function getUser(id: string) {
  try {
    return await request(`/api/users/${id}`);
  } catch (error) {
    console.error('获取用户失败:', error);
    throw error;
  }
}
```

### 3. Component层 (React组件)

**目录**: `lims-web-ui/src/pages/`

**审查重点**:
- Props接口定义
- 状态管理规范
- 列表渲染key属性
- 组件生命周期

**示例规则**:
```typescript
// ❌ 不符合规范
const UserList: React.FC = (props: any) => {
  const [users, setusers] = useState([]);
  
  return users.map(user => (
    <div>{user.name}</div>  // 缺少key属性
  ));
}

// ✅ 符合规范
interface IUserListProps {
  filter?: string;
}

const UserList: React.FC<IUserListProps> = ({ filter }) => {
  const [users, setUsers] = useState<IUser[]>([]);
  
  return users.map(user => (
    <div key={user.id}>{user.name}</div>
  ));
}
```

### 4. Utility层 (工具函数)

**目录**: `lims-web-ui/src/utils/`

**审查重点**:
- 函数导出规范
- 常量命名规范
- 工具函数复用性
- 配置管理

**示例规则**:
```typescript
// ❌ 不符合规范
const apiUrl = 'http://localhost:3000';

function formatDate(date: Date) {
  return date.toISOString();
}

// ✅ 符合规范
export const API_URL = 'http://localhost:3000';

export function formatDate(date: Date): string {
  return date.toISOString();
}
```

## 🔧 自动修复功能

### 支持的自动修复类型

| 问题类型 | 修复内容 | 示例 |
|---------|---------|------|
| 接口命名 | 添加`I`前缀 | `User` → `IUser` |
| 枚举命名 | 添加`E`前缀 | `Status` → `EStatus` |
| 函数导出 | 添加`export`关键字 | `function` → `export function` |
| 常量命名 | 转换为大写蛇形 | `apiUrl` → `API_URL` |
| Props类型 | 添加接口定义 | `props: any` → `props: IProps` |

### 手动修复建议

对于无法自动修复的问题，工具会提供详细的修复建议：

```typescript
// 审查报告中的建议
- ⚠️ WARN: 异步函数建议添加错误处理逻辑
  ↳ 文件: `userService.ts`
  ↳ 建议: 添加try-catch块处理错误

// 修复后
export async function getUser(id: string) {
  try {
    return await request(`/api/users/${id}`);
  } catch (error) {
    console.error('获取用户失败:', error);
    throw error;
  }
}
```

## 📊 CI/CD集成

### GitHub Actions工作流

项目配置了自动化的CI检查：

```yaml
# .github/workflows/typescript-review.yml
name: TypeScript Code Review

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]
```

### PR自动审查

当PR包含TypeScript文件变更时，会自动运行审查：

1. **审查通过**: 显示绿色检查标记
2. **审查失败**: 
   - 显示详细错误报告
   - 提供修复建议
   - 阻断合并直到问题解决

### 跳过审查机制

在紧急情况下可以跳过审查：

```bash
# 在commit消息中添加[skip-review]
git commit -m "紧急修复: 处理生产问题 [skip-review]"
```

## 🎯 最佳实践

### 1. 开发流程

```bash
# 日常开发流程
1. 编写代码
2. 提交前运行审查
3. 自动修复问题
4. 手动处理复杂问题
5. 提交代码
```

### 2. 代码规范

**命名约定**:
- 接口: `I` + 帕斯卡命名 (`IUserProps`)
- 枚举: `E` + 帕斯卡命名 (`EUserRole`)
- 组件: 帕斯卡命名 (`UserList`)
- 服务函数: CRUD前缀 (`getUser`, `createUser`)
- 常量: 大写蛇形 (`API_URL`)

**类型安全**:
```typescript
// ✅ 推荐
interface IUser {
  id: string;
  name: string | null;  // 明确的可空类型
  age?: number;         // 可选属性
}

// ❌ 避免
interface User {
  id: any;
  name?: string;        // 不明确的空值处理
}
```

### 3. 错误处理

```typescript
// ✅ 完整的错误处理
export async function fetchData() {
  try {
    const response = await api.get('/data');
    return response.data;
  } catch (error) {
    // 记录错误日志
    console.error('API请求失败:', error);
    
    // 用户友好的错误消息
    throw new Error('数据加载失败，请稍后重试');
  }
}
```

## 🔍 故障排除

### 常见问题

**Q: 审查工具报错 "目录不存在"**
A: 检查项目结构，确保TypeScript文件在正确目录中

**Q: 自动修复后代码格式混乱**
A: 运行 `npm run format` 重新格式化代码

**Q: CI检查失败但本地通过**
A: 检查GitHub Actions日志，确认环境一致性

**Q: 某些规则误报**
A: 在审查报告中查看具体规则，考虑调整规则配置

### 调试模式

```bash
# 启用详细日志
DEBUG=typescript-reviewer node scripts/typescript-reviewer.mjs

# 检查特定文件
node scripts/typescript-reviewer.mjs --dry-run | grep "文件名"
```

## 📈 性能优化

### 审查范围控制

```bash
# 只审查特定层级
# (当前版本支持自动层级检测)

# 排除特定文件
# 在.gitignore中添加:
# docs/runbook/typescript-review-report.md
```

### 缓存策略

工具会自动缓存扫描结果，避免重复审查未变更文件。

## 🤝 贡献指南

### 添加新规则

1. 在 `REVIEW_RULES` 对象中添加新规则
2. 定义匹配模式和修复逻辑
3. 测试规则有效性
4. 更新本文档

### 规则模板

```javascript
'rule-id': {
  layer: 'type|service|component|utility',
  pattern: /匹配模式/,
  severity: SEVERITY.ERROR|WARN|INFO,
  message: '问题描述',
  autoFix: (content, ...matchGroups) => {
    // 自动修复逻辑
    return fixedContent;
  }
}
```

## 📞 支持与反馈

- **问题报告**: GitHub Issues
- **功能请求**: Feature Requests
- **文档更新**: Pull Requests
- **紧急支持**: 项目维护者

---

*🎯 保持代码质量，从每一次提交开始！*