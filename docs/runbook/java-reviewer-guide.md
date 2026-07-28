# Java Reviewer使用指南

## 📋 概述

`java-reviewer` 是Material LIMS项目的Java代码审查与自动修复工具，基于四层架构设计，提供智能代码质量检查和自动修复功能。

## 🎯 功能特性

### 审查范围
- **Model层**: Entity/DTO/VO/Enum规范检查
- **DAO层**: Mapper接口完整性检查  
- **Service层**: 事务注解和业务逻辑检查
- **Controller层**: 安全和参数校验检查

### 自动修复
支持30+种常见问题自动修复：
- 注解缺失（@TableName、@Mapper、@Transactional等）
- 继承错误（BaseEntity、BaseMapper）
- 命名规范检查
- 参数校验注解添加

### 集成方式
- **本地开发**: Git pre-commit钩子
- **CI/CD**: GitHub Actions自动审查
- **手动运行**: 命令行工具

## 🚀 快速开始

### 1. 安装Git钩子
```bash
# 安装pre-commit钩子
./scripts/setup-hooks.sh
```

### 2. 手动运行审查
```bash
# 基础审查（报告模式）
node scripts/java-reviewer.mjs

# 预览模式（不实际修复）
node scripts/java-reviewer.mjs --dry-run

# CI强制模式（ERROR阻断）
node scripts/java-reviewer.mjs --strict

# 仅修复不报告
node scripts/java-reviewer.mjs --fix-only

# 显示帮助
node scripts/java-reviewer.mjs --help
```

### 3. Git集成使用
```bash
# 正常提交（自动触发审查）
git add .
git commit -m "feat: 新功能"

# 跳过审查（紧急情况）
git commit --no-verify -m "紧急修复"
```

## 📊 审查规则

### Model层规则
| 规则ID | 严重级 | 描述 | 自动修复 |
|--------|--------|------|----------|
| model-entity-inheritance | ⛔ ERROR | Entity必须继承BaseEntity | ✅ |
| model-entity-table-name | ⛔ ERROR | Entity缺少@TableName注解 | ✅ |
| model-entity-equals-hashcode | ⚠️ WARN | Entity缺少@EqualsAndHashCode | ✅ |
| model-dto-validation | ⚠️ WARN | DTO字段建议添加校验注解 | 🔄 建议 |

### DAO层规则
| 规则ID | 严重级 | 描述 | 自动修复 |
|--------|--------|------|----------|
| dao-mapper-annotation | ⛔ ERROR | Mapper缺少@Mapper注解 | ✅ |
| dao-base-mapper-inheritance | ⛔ ERROR | Mapper必须继承BaseMapper | ✅ |

### Service层规则
| 规则ID | 严重级 | 描述 | 自动修复 |
|--------|--------|------|----------|
| service-transactional | ⚠️ WARN | Service建议添加@Transactional | ✅ |

### Controller层规则
| 规则ID | 严重级 | 描述 | 自动修复 |
|--------|--------|------|----------|
| controller-security | ⚠️ WARN | Controller建议添加安全注解 | 🔄 建议 |

## 🔧 配置说明

### 目录结构
```
scripts/
├── java-reviewer.mjs          # 主审查脚本
├── setup-hooks.sh              # 钩子安装脚本
└── uninstall-hooks.sh          # 钩子卸载脚本

.github/workflows/
└── java-review.yml             # GitHub Actions配置

docs/runbook/
└── java-review-report.md       # 审查报告（自动生成）
```

### 审查报告
每次运行会生成详细报告：
- 问题统计（按层级分类）
- 详细问题列表
- 自动修复记录
- CI模式结果

## 🛠️ 最佳实践

### 开发流程
1. **本地开发时**: 依赖pre-commit钩子自动审查
2. **提交前**: 手动运行`node scripts/java-reviewer.mjs`检查
3. **CI/CD**: GitHub Actions自动审查，ERROR阻断合并
4. **定期**: 每周一自动运行全面审查

### 问题处理
- **ERROR级别**: 必须修复，否则阻断提交/合并
- **WARN级别**: 建议修复，不影响流程
- **自动修复**: 优先使用自动修复功能
- **手动修复**: 复杂问题需要人工干预

### 团队协作
- 新成员入职时运行`setup-hooks.sh`
- 定期审查报告共享给团队
- 重大重构前运行全面审查

## 🚨 故障排除

### 常见问题

**Q: 钩子不生效？**
A: 检查钩子文件权限：
```bash
ls -la .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

**Q: 审查太慢？**
A: 使用`--fix-only`模式仅修复不生成报告

**Q: 误报问题？**
A: 使用`git commit --no-verify`跳过单次审查

**Q: 卸载钩子？**
A: 运行`./scripts/uninstall-hooks.sh`

### 调试模式
```bash
# 查看详细日志
export DEBUG_JAVA_REVIEWER=true
node scripts/java-reviewer.mjs
```

## 📈 性能优化

### 增量审查
工具会自动检测变更文件，只审查有改动的Java文件。

### 缓存机制
审查结果会缓存，避免重复检查未修改的文件。

### 并行处理
大型项目支持多文件并行审查，提升效率。

## 🔮 未来规划

- [ ] 支持自定义审查规则
- [ ] 集成SonarQube质量门禁
- [ ] 添加代码复杂度检查
- [ ] 支持更多Java框架（Spring Boot、Quarkus等）
- [ ] 可视化审查报告界面

---

**维护者**: Material LIMS开发团队  
**更新日期**: 2026-06-24  
**版本**: v1.0.0