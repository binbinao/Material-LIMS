# ECC (EllAI Code Companion) 使用指南

## 概述

ECC (EllAI Code Companion) 是一套完整的 AI 编程助手扩展集合，已安装到用户级 OpenCode 配置 (`~/.config/opencode/`)。

## 已安装组件

### Commands (命令)

位于 `~/.config/opencode/commands/`，共 37 个命令：

| 命令 | 描述 |
|------|------|
| `build-fix.md` | 构建修复 |
| `checkpoint.md` | 检查点管理 |
| `code-review.md` | 代码审查 |
| `e2e.md` | 端到端测试 |
| `eval.md` | 评估测试 |
| `evolve.md` | 迭代演化 |
| `go-build.md` | Go 构建 |
| `go-review.md` | Go 代码审查 |
| `go-test.md` | Go 测试 |
| `harness-audit.md` | 工具链审计 |
| `instinct-export.md` | 直觉导出 |
| `instinct-import.md` | 直觉导入 |
| `instinct-status.md` | 直觉状态 |
| `learn.md` | 学习模式 |
| `loop-start.md` | 循环启动 |
| `loop-status.md` | 循环状态 |
| `model-route.md` | 模型路由 |
| `orchestrate.md` | 编排 |
| `plan.md` | 计划 |
| `projects.md` | 项目管理 |

### Tools (工具)

位于 `~/.config/opencode/tools/`，共 11 个工具：

| 工具 | 描述 |
|------|------|
| `*/` | 各类自定义工具 |

### Plugins (插件)

位于 `~/.config/opencode/plugins/`，提供扩展功能。

### Prompts (提示词)

位于 `~/.config/opencode/prompts/`，预设提示词模板。

## Skills (技能) 目录

ECC 源码库 (`/tmp/ECC/skills/`) 包含 **273 个技能**，涵盖：

### 核心技能分类

| 分类 | 技能数量 | 示例 |
|------|----------|------|
| Framework & Language | 25+ | Django, Laravel, Spring Boot, Go, Python, Java |
| Database | 3 | PostgreSQL, ClickHouse, JPA |
| Workflow & Quality | 8 | TDD, verification, security-review |
| Research & APIs | 2 | deep-research, exa-search |
| Business & Content | 5 | article-writing, market-research |
| Media Generation | 2 | fal-ai-media, video-editing |
| Orchestration | 1 | dmux-workflows |

### 常用技能推荐

#### 后端开发
- `backend-patterns` - 后端架构与 API 设计
- `springboot-patterns` - Spring Boot 架构
- `python-patterns` - Python 最佳实践
- `golang-patterns` - Go 惯用写法

#### 测试与质量
- `tdd-workflow` - TDD 工作流
- `verification-loop` - 验证循环
- `security-review` - 安全审查
- `eval-harness` - 评估框架

#### 前端开发
- `frontend-patterns` - React/Next.js 模式
- `frontend-slides` - HTML 演示文稿

#### 研究与搜索
- `deep-research` - 深度研究
- `exa-search` - 神经网络搜索

#### 内容创作
- `article-writing` - 长文写作
- `content-engine` - 多平台内容分发
- `market-research` - 市场调研

## 使用方式

### 调用 Skills

在 OpenCode 中使用 Skill 工具加载：

```
使用 skill 工具调用 <skill-name>
```

例如：
- `skill(name="tdd-workflow")` - 启用 TDD 工作流
- `skill(name="security-review")` - 启用安全审查

### 使用 Commands

直接在对话中使用斜杠命令（如果 OpenCode 支持）：

```
/code-review
/e2e
/plan
```

## 安装更多 Skills

如需从 ECC 安装更多技能到用户配置：

```bash
# 复制技能到用户配置
cp -R /tmp/ECC/skills/<skill-name> ~/.config/opencode/skills/

# 复制规则
cp -R /tmp/ECC/rules/common ~/.config/opencode/rules/
```

## 相关文件

- ECC 源码：`/tmp/ECC/`
- 用户配置：`~/.config/opencode/`
- 项目配置：`.opencode/`
- ECC 官方文档：`/tmp/ECC/README.md`

## 参考链接

- [ECC GitHub](https://github.com/affaan-m/ECC)
- [ECC 安装指南](https://github.com/affaan-m/ECC/blob/main/.opencode/README.md)
