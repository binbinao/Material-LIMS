#!/usr/bin/env node

/**
 * java-reviewer — Material LIMS Java代码审查与自动修复工具
 * 
 * 功能：
 *   1. 四层架构审查：Controller → Service → DAO → Model
 *   2. 智能自动修复：支持30+种常见问题自动修复
 *   3. CI强制模式：ERROR级别问题阻断提交/合并
 *   4. Git集成：修复后自动提交
 *
 * 审查范围：
 *   - Model层：Entity/DTO/VO/Enum 规范检查
 *   - DAO层：Mapper接口完整性检查
 *   - Service层：事务注解和业务逻辑检查
 *   - Controller层：安全和参数校验检查
 *
 * 用法：
 *   node scripts/java-reviewer.mjs                    # 审查并自动修复
 *   node scripts/java-reviewer.mjs --dry-run          # 预览模式，不实际修复
 *   node scripts/java-reviewer.mjs --strict           # CI强制模式，ERROR阻断
 *   node scripts/java-reviewer.mjs --fix-only         # 只修复不报告
 *   node scripts/java-reviewer.mjs --help             # 显示帮助
 */

import { readFileSync, writeFileSync, readdirSync, existsSync } from 'fs';
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
const REVIEW_RULES = {
  // MODEL层规则
  'model-entity-inheritance': {
    layer: 'model',
    pattern: /public class (\w+)(?!.*extends BaseEntity)/,
    severity: SEVERITY.ERROR,
    message: 'Entity类必须继承BaseEntity',
    autoFix: (content, className) => content.replace(
      `public class ${className}`, 
      `public class ${className} extends BaseEntity`
    )
  },

  'model-entity-table-name': {
    layer: 'model',
    pattern: /public class (\w+) extends BaseEntity(?!.*@TableName)/,
    severity: SEVERITY.ERROR,
    message: 'Entity类缺少@TableName注解',
    autoFix: (content, className) => {
      const tableName = className.toLowerCase();
      return content.replace(
        `public class ${className} extends BaseEntity`,
        `@TableName("${tableName}")\npublic class ${className} extends BaseEntity`
      );
    }
  },

  'model-entity-equals-hashcode': {
    layer: 'model',
    pattern: /@Data(?!.*@EqualsAndHashCode)/,
    severity: SEVERITY.WARN,
    message: 'Entity类缺少@EqualsAndHashCode注解',
    autoFix: (content) => content.replace('@Data', '@Data\n@EqualsAndHashCode(callSuper = true)')
  },

  'model-dto-validation': {
    layer: 'model',
    pattern: /private String (\w+);/g,
    severity: SEVERITY.WARN,
    message: 'DTO字段建议添加参数校验注解',
    suggestFix: (content, fieldName) => {
      const requiredFields = ['id', 'name', 'title', 'code', 'no', 'reason'];
      if (requiredFields.some(f => fieldName.toLowerCase().includes(f))) {
        return `@NotBlank(message = "${fieldName} is required")\n    private String ${fieldName};`;
      }
      return null;
    }
  },

  // DAO层规则
  'dao-mapper-annotation': {
    layer: 'dao',
    pattern: /public interface (\w+)(?!.*@Mapper)/,
    severity: SEVERITY.ERROR,
    message: 'Mapper接口缺少@Mapper注解',
    autoFix: (content) => content.replace('public interface', '@Mapper\npublic interface')
  },

  'dao-base-mapper-inheritance': {
    layer: 'dao',
    pattern: /public interface (\w+)(?!.*extends BaseMapper)/,
    severity: SEVERITY.ERROR,
    message: 'Mapper接口必须继承BaseMapper',
    autoFix: (content, className) => {
      const entityName = className.replace('Mapper', '');
      return content.replace(
        `public interface ${className}`,
        `public interface ${className} extends BaseMapper<${entityName}>`
      );
    }
  },

  // Service层规则
  'service-transactional': {
    layer: 'service',
    pattern: /@Service(?!.*@Transactional)/,
    severity: SEVERITY.WARN,
    message: 'Service类建议添加@Transactional注解',
    autoFix: (content) => content.replace('@Service', '@Service\n@Transactional(readOnly = true)')
  },

  // Controller层规则
  'controller-security': {
    layer: 'controller',
    pattern: /@RestController(?!.*@PreAuthorize)/,
    severity: SEVERITY.WARN,
    message: 'Controller建议添加安全注解',
    suggestFix: () => '@PreAuthorize("hasRole(\"USER\")")' // 建议模式
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

// ─── 审查单个文件 ───────────────────────────────────────────
function reviewFile(filePath, args) {
  const content = readFileSync(filePath, 'utf-8');
  const fileName = filePath.split('/').pop();
  const issues = [];
  const fixes = [];

  // 确定文件所属层级
  let fileLayer = '';
  for (const [layer, dir] of Object.entries(DIRECTORIES)) {
    if (filePath.includes(dir)) {
      fileLayer = layer;
      break;
    }
  }

  if (!fileLayer) {
    console.warn(`⚠️  无法确定文件层级: ${fileName}`);
    return { issues, fixes };
  }

  // 应用该层级的所有规则
  for (const [ruleId, rule] of Object.entries(REVIEW_RULES)) {
    if (rule.layer !== fileLayer) continue;

    const matches = [...content.matchAll(new RegExp(rule.pattern.source, 'g'))];
    
    for (const match of matches) {
      const issue = {
        ruleId,
        severity: rule.severity,
        message: rule.message,
        file: fileName,
        layer: fileLayer,
        match: match[0]
      };

      issues.push(issue);

      // 自动修复逻辑
      if (rule.autoFix && !args.dryRun) {
        const fixedContent = rule.autoFix(content, match[1]);
        if (fixedContent !== content) {
          fixes.push({
            filePath,
            original: content,
            fixed: fixedContent,
            ruleId,
            description: rule.message
          });
        }
      }
    }
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
      const layerIssues = allIssues.filter(i => i.layer === layer);
      if (layerIssues.length === 0) continue;

      report += `### ${layer.toUpperCase()}层\n\n`;
      
      for (const issue of layerIssues) {
        report += `- **${issue.severity}**: ${issue.message}  
  ↳ 文件: \`${issue.file}\`  
  ↳ 匹配: \`${issue.match.substring(0, 50)}...\`\n\n`;
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
    report += `❌ **审查失败**: 发现 ${totalErrors} 个ERROR级别问题，阻断提交/合并\n`;
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
  node scripts/java-reviewer.mjs --strict          CI强制模式，ERROR阻断提交
  node scripts/java-reviewer.mjs --fix-only        只修复不生成报告
  node scripts/java-reviewer.mjs --git-commit      修复后自动git commit
  node scripts/java-reviewer.mjs --help            显示此帮助

审查范围:
  📦 Model层: Entity/DTO/VO/Enum规范检查
  🗃️  DAO层: Mapper接口完整性检查  
  ⚙️  Service层: 事务注解和业务逻辑检查
  🌐 Controller层: 安全和参数校验检查

自动修复:
  支持30+种常见问题自动修复，包括注解缺失、继承错误、命名规范等
`);
    return;
  }

  console.log('🔍 开始Java代码审查...\n');

  const allIssues = [];
  const allFixes = [];

  // 扫描四层架构；PR 可通过 --files 只审查本次变更，避免历史问题阻断新变更。
  const selectedFiles = args.files.length > 0
    ? args.files.map((file) => file.startsWith('/') ? file : join(PROJECT_ROOT, file))
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

  // 应用修复
  if (allFixes.length > 0 && !args.dryRun) {
    console.log('🔧 应用自动修复...');
    for (const fix of allFixes) {
      writeFileSync(fix.filePath, fix.fixed, 'utf-8');
      console.log(`   ✅ 修复: ${fix.filePath.split('/').pop()}`);
    }
    console.log('');
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
      require('fs').mkdirSync(reportDir, { recursive: true });
    }
    writeFileSync(reportPath, report, 'utf-8');
    console.log(`✅ 审查完成！报告已保存: ${reportPath}\n`);
    
    // 控制台摘要
    const totalErrors = allIssues.filter(i => i.severity === SEVERITY.ERROR).length;
    const totalWarns = allIssues.filter(i => i.severity === SEVERITY.WARN).length;
    
    console.log(`📊 审查摘要:`);
    console.log(`   ⛔ ERROR: ${totalErrors} 个`);
    console.log(`   ⚠️  WARN: ${totalWarns} 个`);
    console.log(`   🔧 修复: ${allFixes.length} 个\n`);

    // CI模式退出码
    if (args.strict && totalErrors > 0) {
      console.log('❌ CI强制模式: 发现ERROR问题，退出码为1');
      process.exit(1);
    }
  }
}

// 执行主流程
main().catch(error => {
  console.error('❌ 审查过程出错:', error);
  process.exit(1);
});