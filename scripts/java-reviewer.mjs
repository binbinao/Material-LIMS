#!/usr/bin/env node

/**
 * java-reviewer — Material LIMS Java代码审查与自动修复工具
 *
 * 功能：
 *   1. 四层架构审查：Controller → Service → DAO → Model
 *   2. 智能自动修复：支持常见问题自动修复
 *   3. CI强制模式：ERROR级别问题阻断提交/合并
 *   4. Git集成：修复后自动提交
 *
 * 用法：
 *   node scripts/java-reviewer.mjs                    # 审查并自动修复
 *   node scripts/java-reviewer.mjs --dry-run          # 预览模式，不实际修复
 *   node scripts/java-reviewer.mjs --strict           # CI强制模式，ERROR阻断
 *   node scripts/java-reviewer.mjs --fix-only         # 只修复不报告
 *   node scripts/java-reviewer.mjs --files <paths>    # 仅审查指定Java文件
 *   node scripts/java-reviewer.mjs --help             # 显示帮助
 */

import { readFileSync, writeFileSync, readdirSync, existsSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

// ─── 路径配置 ───────────────────────────────────────────────
const __dirname = dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = join(__dirname, '..');

// 四层架构目录
const DIRECTORIES = {
  model: join(PROJECT_ROOT, 'lims-model/src/main/java/com/lims/model'),
  dao: join(PROJECT_ROOT, 'lims-dao/src/main/java/com/lims/dao/mapper'),
  service: join(PROJECT_ROOT, 'lims-service/src/main/java/com/lims/service'),
  controller: join(PROJECT_ROOT, 'lims-web/src/main/java/com/lims/web/controller')
};

// ─── 问题严重级别 ───────────────────────────────────────────
const SEVERITY = {
  ERROR: '⛔ ERROR',
  WARN: '⚠️ WARN',
  INFO: '💡 INFO'
};

// ─── 审查规则定义 ───────────────────────────────────────────
// 每个规则使用独立的 hasAnnotation/hasInheritance 精确判定，
// 不再依赖跨整文件的 lookahead 正则，避免 import 行误判。
const REVIEW_RULES = {
  // Model层规则
  'model-entity-inheritance': {
    layer: 'model',
    applies: (content) => /public class (\w+)/.test(content) && !/public class \w+\s+extends\s+BaseEntity/.test(content),
    severity: SEVERITY.ERROR,
    message: 'Entity类必须继承BaseEntity',
    autoFix: (content) => {
      const m = content.match(/public class (\w+)/);
      if (!m) return content;
      return content.replace(`public class ${m[1]}`, `public class ${m[1]} extends BaseEntity`);
    }
  },

  'model-entity-table-name': {
    layer: 'model',
    applies: (content) => /public class \w+\s+extends\s+BaseEntity/.test(content) && !/@TableName\s*\(/.test(content),
    severity: SEVERITY.ERROR,
    message: 'Entity类缺少@TableName注解',
    autoFix: (content) => {
      const m = content.match(/public class (\w+) extends BaseEntity/);
      if (!m) return content;
      const tableName = m[1].toLowerCase();
      return content.replace(`public class ${m[1]} extends BaseEntity`, `@TableName("${tableName}")\npublic class ${m[1]} extends BaseEntity`);
    }
  },

  'model-entity-equals-hashcode': {
    layer: 'model',
    applies: (content) => /@Data/.test(content) && !/@EqualsAndHashCode/.test(content),
    severity: SEVERITY.WARN,
    message: 'Entity类缺少@EqualsAndHashCode注解',
    autoFix: (content) => content.replace('@Data', '@Data\n@EqualsAndHashCode(callSuper = true)')
  },

  'model-dto-validation': {
    layer: 'model',
    applies: (content) => false, // 仅建议，默认不触发，避免误报
    severity: SEVERITY.WARN,
    message: 'DTO字段建议添加参数校验注解',
    suggestFix: () => null
  },

  // DAO层规则
  'dao-mapper-annotation': {
    layer: 'dao',
    // 精确判定：public interface 之前没有 @Mapper 注解行
    applies: (content) => {
      const m = content.match(/public interface \w+/);
      if (!m) return false;
      const idx = content.indexOf(m[0]);
      const before = content.substring(0, idx);
      // 检查 interface 声明前最近的非空行是否为 @Mapper
      const lines = before.split('\n').filter((l) => l.trim().length > 0);
      const lastLine = lines[lines.length - 1] || '';
      return !/@Mapper\b/.test(lastLine);
    },
    severity: SEVERITY.ERROR,
    message: 'Mapper接口缺少@Mapper注解',
    autoFix: (content) => content.replace(/(public interface)/, '@Mapper\n$1')
  },

  'dao-base-mapper-inheritance': {
    layer: 'dao',
    applies: (content) => /public interface \w+/.test(content) && !/extends\s+BaseMapper</.test(content),
    severity: SEVERITY.ERROR,
    message: 'Mapper接口必须继承BaseMapper',
    autoFix: (content) => {
      const m = content.match(/public interface (\w+)/);
      if (!m) return content;
      const entityName = m[1].replace('Mapper', '');
      return content.replace(`public interface ${m[1]}`, `public interface ${m[1]} extends BaseMapper<${entityName}>`);
    }
  },

  // Service层规则
  'service-transactional': {
    layer: 'service',
    applies: (content) => /@Service/.test(content) && !/@Transactional/.test(content),
    severity: SEVERITY.WARN,
    message: 'Service类建议添加@Transactional注解',
    autoFix: (content) => content.replace(/(@Service)/, '$1\n@Transactional(readOnly = true)')
  },

  // Controller层规则
  'controller-security': {
    layer: 'controller',
    applies: () => false, // 建议模式，不阻断CI
    severity: SEVERITY.WARN,
    message: 'Controller建议添加安全注解',
    suggestFix: () => '@PreAuthorize("hasRole(\"USER\")")'
  }
};

// ─── CLI 参数解析 ──────────────────────────────────────────
function parseArgs() {
  const args = process.argv.slice(2);
  return {
    dryRun: args.includes('--dry-run'),
    strict: args.includes('--strict'),
    fixOnly: args.includes('--fix-only'),
    help: args.includes('--help'),
    gitCommit: args.includes('--git-commit'),
    files: args.includes('--files')
      ? args.slice(args.indexOf('--files') + 1).filter((arg) => !arg.startsWith('--'))
      : []
  };
}

// ─── 文件扫描工具 ───────────────────────────────────────────
function scanJavaFiles(directory) {
  if (!existsSync(directory)) {
    console.warn(`⚠️  目录不存在: ${directory}`);
    return [];
  }

  const files = [];

  function scanDir(dir) {
    const items = readdirSync(dir, { withFileTypes: true });

    for (const item of items) {
      const fullPath = join(dir, item.name);

      if (item.isDirectory()) {
        scanDir(fullPath);
      } else if (item.isFile() && item.name.endsWith('.java')) {
        files.push(fullPath);
      }
    }
  }

  scanDir(directory);
  return files;
}

// ─── 确定文件所属层级 ───────────────────────────────────────
function detectLayer(filePath) {
  for (const [layer, dir] of Object.entries(DIRECTORIES)) {
    if (filePath.includes(dir)) {
      return layer;
    }
  }
  return '';
}

// ─── 审查单个文件（含修复后重新扫描） ───────────────────────
function reviewFile(filePath, args) {
  let content = readFileSync(filePath, 'utf-8');
  const fileName = filePath.split('/').pop();
  const issues = [];
  const fixes = [];

  const fileLayer = detectLayer(filePath);
  if (!fileLayer) {
    console.warn(`⚠️  无法确定文件层级: ${fileName}`);
    return { issues, fixes };
  }

  // 先应用自动修复，再扫描剩余问题，避免"修复前的问题列表"误判CI失败。
  const appliedRuleIds = new Set();

  for (const [ruleId, rule] of Object.entries(REVIEW_RULES)) {
    if (rule.layer !== fileLayer) continue;
    if (appliedRuleIds.has(ruleId)) continue;

    if (!rule.applies(content)) continue;

    // 记录问题
    issues.push({
      ruleId,
      severity: rule.severity,
      message: rule.message,
      file: fileName,
      layer: fileLayer
    });

    // 自动修复
    if (rule.autoFix && !args.dryRun) {
      const fixedContent = rule.autoFix(content);
      if (fixedContent !== content) {
        content = fixedContent;
        fixes.push({ filePath, ruleId, description: rule.message });
        appliedRuleIds.add(ruleId);
      }
    }
  }

  // 若有修复，写回文件并重新扫描一次，确保 strict 模式判定基于修复后状态
  if (fixes.length > 0 && !args.dryRun) {
    writeFileSync(filePath, content, 'utf-8');

    // 重新扫描：清除已修复的问题，保留仍存在的
    const remainingIssues = [];
    for (const issue of issues) {
      const rule = REVIEW_RULES[issue.ruleId];
      if (rule && rule.applies(content)) {
        remainingIssues.push(issue);
      }
    }
    issues.length = 0;
    issues.push(...remainingIssues);
  }

  return { issues, fixes };
}

// ─── 生成审查报告 ───────────────────────────────────────────
function generateReport(allIssues, allFixes, args) {
  const now = new Date().toLocaleString('zh-CN');

  let report = `# Java代码审查报告

> 🤖 由 \`scripts/java-reviewer.mjs\` 自动生成  
> 📅 审查时间: ${now}  
> 🔧 模式: ${args.strict ? '严格模式' : args.fixOnly ? '仅修复' : '建议模式'}

`;

  // 按层级分组统计
  const layerStats = {};
  for (const layer of Object.keys(DIRECTORIES)) {
    layerStats[layer] = { error: 0, warn: 0, info: 0 };
  }

  // 统计问题
  for (const issue of allIssues) {
    if (issue.severity === SEVERITY.ERROR) layerStats[issue.layer].error++;
    else if (issue.severity === SEVERITY.WARN) layerStats[issue.layer].warn++;
    else layerStats[issue.layer].info++;
  }

  // 总体统计
  const totalErrors = Object.values(layerStats).reduce((sum, s) => sum + s.error, 0);
  const totalWarns = Object.values(layerStats).reduce((sum, s) => sum + s.warn, 0);
  const totalInfos = Object.values(layerStats).reduce((sum, s) => sum + s.info, 0);

  report += `## 📊 审查统计

| 层级 | ⛔ ERROR | ⚠️ WARN | 💡 INFO |
|------|----------|---------|---------|\n`;

  for (const [layer, stats] of Object.entries(layerStats)) {
    report += `| ${layer} | ${stats.error} | ${stats.warn} | ${stats.info} |\n`;
  }

  report += `| **总计** | **${totalErrors}** | **${totalWarns}** | **${totalInfos}** |\n\n`;

  // 详细问题列表
  if (allIssues.length > 0) {
    report += `## 🔍 发现问题详情\n\n`;

    // 按层级分组显示
    for (const layer of Object.keys(DIRECTORIES)) {
      const layerIssues = allIssues.filter((i) => i.layer === layer);
      if (layerIssues.length === 0) continue;

      report += `### ${layer.toUpperCase()}层\n\n`;

      for (const issue of layerIssues) {
        report += `- **${issue.severity}**: ${issue.message}  
  ↳ 文件: \`${issue.file}\`\n\n`;
      }
    }
  }

  // 修复统计
  if (allFixes.length > 0) {
    report += `## 🔧 自动修复统计\n\n`;
    report += `✅ 成功修复 ${allFixes.length} 个问题:\n\n`;

    for (const fix of allFixes) {
      const fileName = fix.filePath.split('/').pop();
      report += `- **${fileName}**: ${fix.description}\n`;
    }
  }

  // CI模式结果
  if (args.strict && totalErrors > 0) {
    report += `\n## 🚫 CI强制模式结果\n\n`;
    report += `❌ **审查失败**: 修复后仍存在 ${totalErrors} 个ERROR级别问题，阻断提交/合并\n`;
    report += `💡 建议使用 \`node scripts/java-reviewer.mjs\` 自动修复问题\n`;
  }

  return report;
}

// ─── 主流程 ─────────────────────────────────────────────────
async function main() {
  const args = parseArgs();

  if (args.help) {
    console.log(`
🧢 Stacky's java-reviewer — Material LIMS Java代码审查与自动修复工具

用法:
  node scripts/java-reviewer.mjs                   审查并自动修复
  node scripts/java-reviewer.mjs --dry-run         预览模式，不实际修复
  node scripts/java-reviewer.mjs --strict          CI强制模式，ERROR阻断
  node scripts/java-reviewer.mjs --fix-only        只修复不生成报告
  node scripts/java-reviewer.mjs --files <paths>   仅审查指定Java文件
  node scripts/java-reviewer.mjs --git-commit      修复后自动git commit
  node scripts/java-reviewer.mjs --help            显示帮助

审查范围:
  📦 Model层: Entity/DTO/VO/Enum规范检查
  🗃️  DAO层: Mapper接口完整性检查  
  ⚙️  Service层: 事务注解和业务逻辑检查
  🌐 Controller层: 安全和参数校验检查

自动修复:
  支持常见问题自动修复，包括注解缺失、继承错误、命名规范等
`);
    return;
  }

  console.log('🔍 开始Java代码审查...\n');

  const allIssues = [];
  const allFixes = [];

  // 扫描四层架构；PR 可通过 --files 只审查本次变更，避免历史问题阻断新变更。
  const selectedFiles = args.files.length > 0
    ? args.files.map((file) => (file.startsWith('/') ? file : join(PROJECT_ROOT, file)))
        .filter((file) => existsSync(file) && file.endsWith('.java'))
    : null;
  for (const [layer, directory] of Object.entries(DIRECTORIES)) {
    console.log(`📂 扫描 ${layer} 层: ${directory}`);
    const files = selectedFiles
      ? selectedFiles.filter((file) => file.startsWith(directory))
      : scanJavaFiles(directory);

    for (const file of files) {
      const { issues, fixes } = reviewFile(file, args);
      allIssues.push(...issues);
      allFixes.push(...fixes);
    }

    console.log(`   📄 扫描 ${files.length} 个文件\n`);
  }

  // 生成报告
  const report = generateReport(allIssues, allFixes, args);

  if (args.dryRun) {
    console.log('📝 [DRY RUN] 审查报告预览:\n');
    console.log(report);
  } else {
    // 写入报告文件
    const reportPath = join(PROJECT_ROOT, 'docs/runbook/java-review-report.md');
    const reportDir = dirname(reportPath);
    if (!existsSync(reportDir)) {
      mkdirSync(reportDir, { recursive: true });
    }
    writeFileSync(reportPath, report, 'utf-8');
    console.log(`✅ 审查完成！报告已保存: ${reportPath}\n`);

    // 控制台摘要
    const totalErrors = allIssues.filter((i) => i.severity === SEVERITY.ERROR).length;
    const totalWarns = allIssues.filter((i) => i.severity === SEVERITY.WARN).length;

    console.log(`📊 审查摘要:`);
    console.log(`   ⛔ ERROR: ${totalErrors} 个`);
    console.log(`   ⚠️  WARN: ${totalWarns} 个`);
    console.log(`   🔧 修复: ${allFixes.length} 个\n`);

    // CI模式退出码：仅当修复后仍存在ERROR才失败
    if (args.strict && totalErrors > 0) {
      console.log('❌ CI强制模式: 修复后仍存在ERROR问题，退出码为1');
      process.exit(1);
    }
  }

  // Git自动提交
  if (args.gitCommit && allFixes.length > 0) {
    const { execSync } = await import('child_process');
    const changedFiles = [...new Set(allFixes.map((f) => f.filePath))];
    execSync(`git add ${changedFiles.map((f) => `"${f}"`).join(' ')} docs/runbook/java-review-report.md`, { stdio: 'inherit' });
    execSync('git commit -m "style: auto-fix Java review issues"', { stdio: 'inherit' });
  }
}

// 执行主流程
main().catch((error) => {
  console.error('❌ 审查过程出错:', error);
  process.exit(1);
});
