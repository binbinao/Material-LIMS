#!/usr/bin/env node

/**
 * doc-updater — Material LIMS 文档自动更新器
 *
 * 功能：
 *   1. 扫描 lims-web/src/main/java/.../controller/ 下所有 Controller.java
 *   2. 解析 Spring MVC 注解（@RequestMapping / @GetMapping / @PostMapping 等）
 *   3. 生成/更新 docs/runbook/api-summary.md
 *   4. 可选：检查 README.md 模块列表与实际代码的一致性
 *
 * 用法：
 *   node scripts/doc-updater.mjs                  # 更新 api-summary.md
 *   node scripts/doc-updater.mjs --dry-run         # 预览，不写入文件
 *   node scripts/doc-updater.mjs --check-readme    # 检查 README.md 是否过时
 *   node scripts/doc-updater.mjs --help            # 显示帮助
 */

import { readFileSync, writeFileSync, readdirSync, existsSync, mkdirSync } from 'fs';
import { join, dirname } from 'path';
import { fileURLToPath } from 'url';

// ─── 路径配置 ───────────────────────────────────────────────
const __dirname = dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = join(__dirname, '..');
const CONTROLLER_DIR = join(PROJECT_ROOT, 'lims-web/src/main/java/com/lims/web/controller');
const API_SUMMARY_PATH = join(PROJECT_ROOT, 'docs/runbook/api-summary.md');
const README_PATH = join(PROJECT_ROOT, 'README.md');

// ─── HTTP 方法 → 简称映射 ─────────────────────────────────
const METHOD_SHORT = {
  'Get': 'GET',
  'Post': 'POST',
  'Put': 'PUT',
  'Delete': 'DEL',
  'Patch': 'PATCH',
};

const METHOD_ORDER = ['GET', 'POST', 'PUT', 'DEL', 'PATCH'];

// ─── CLI 参数解析 ──────────────────────────────────────────
function parseArgs() {
  const args = process.argv.slice(2);
  return {
    dryRun: args.includes('--dry-run'),
    checkReadme: args.includes('--check-readme'),
    syncReadme: args.includes('--sync-readme'),
    help: args.includes('--help'),
  };
}

// ─── 正则工具 ──────────────────────────────────────────────
function extractFirst(pattern, text) {
  const m = text.match(pattern);
  return m ? m[1].trim() : null;
}

function extractAll(pattern, text) {
  const results = [];
  let m;
  while ((m = pattern.exec(text)) !== null) {
    results.push(m[1].trim());
  }
  return results;
}

// ─── 解析单个 Controller 文件 ─────────────────────────────
function parseControllerFile(filePath) {
  const content = readFileSync(filePath, 'utf-8');
  const fileName = filePath.split('/').pop();

  // 提取类级信息
  const tagName = extractFirst(/@Tag\s*\(\s*name\s*=\s*"([^"]+)"/s, content);
  const tagDesc = extractFirst(/@Tag\s*\([^)]*description\s*=\s*"([^"]+)"/s, content);
  const basePath = extractFirst(/@RequestMapping\s*\(\s*"([^"]+)"/, content);

  if (!basePath) return null; // 不是 Controller

  // 按方法边界分割
  const methodBlocks = content.split(/(?=\s+@(?:Get|Post|Put|Delete|Patch)Mapping|\s+@Operation)/m);

  const endpoints = [];
  // 用更精确的方式：找到所有方法注解
  // 找所有方法级映射注解（更健壮的正则）
  // 支持三种形式：
  //   1. @GetMapping                → 无括号，无路径
  //   2. @GetMapping("/path")       → 直接路径
  //   3. @PostMapping(value="/p")   → value= 形式
  //   4. @PostMapping(consumes="..")→ 只有其他参数，无路径
  const methodPattern = /@(Get|Post|Put|Delete|Patch)Mapping\s*(?:\(([^)]*)\))?/g;
  const methodMatches = [];
  let mm;
  while ((mm = methodPattern.exec(content)) !== null) {
    const httpMethod = mm[1];
    const argStr = mm[2] || '';
    let path = '';

    // 尝试从参数中提取路径
    if (argStr) {
      // 直接路径字符串: @GetMapping("/foo")
      const directPath = argStr.match(/^\s*"([^"]*)"\s*$/);
      if (directPath) {
        path = directPath[1];
      } else {
        // value="/foo" 形式
        const valuePath = argStr.match(/value\s*=\s*"([^"]*)"/);
        if (valuePath) {
          path = valuePath[1];
        }
        // 如果只有 consumes/produces 等参数没有 path，path 留空
      }
    }

    methodMatches.push({
      httpMethod,
      path,
      index: mm.index,
    });
  }

  // 找 @Operation
  const opPattern = /@Operation\s*\([^)]*summary\s*=\s*"([^"]+)"/g;
  const opMatches = [];
  let om;
  while ((om = opPattern.exec(content)) !== null) {
    opMatches.push({
      summary: om[1],
      index: om.index,
    });
  }

  // 为每个方法匹配最近的 @Operation
  for (const method of methodMatches) {
    let summary = method.httpMethod + ' ' + (method.path || '/');
    let closestOp = null;
    for (const op of opMatches) {
      if (op.index < method.index && (closestOp === null || op.index > closestOp.index)) {
        closestOp = op;
      }
      if (op.index > method.index) break;
    }
    if (closestOp && (method.index - closestOp.index) < 200) {
      summary = closestOp.summary;
    }

    endpoints.push({
      method: METHOD_SHORT[method.httpMethod] || method.httpMethod.toUpperCase(),
      path: method.path || '',
      summary,
    });
  }

  return {
    tagName: tagName || fileName.replace('.java', ''),
    tagDesc: tagDesc || '',
    basePath,
    endpoints,
  };
}

// ─── 扫描所有 Controller ───────────────────────────────────
function scanControllers() {
  if (!existsSync(CONTROLLER_DIR)) {
    console.error(`❌ Controller 目录不存在: ${CONTROLLER_DIR}`);
    process.exit(1);
  }

  const files = readdirSync(CONTROLLER_DIR).filter(f => f.endsWith('.java'));
  const controllers = [];

  for (const file of files.sort()) {
    const result = parseControllerFile(join(CONTROLLER_DIR, file));
    if (result) controllers.push(result);
  }

  return controllers;
}

// ─── 生成 api-summary.md ───────────────────────────────────
function generateApiSummary(controllers) {
  const now = new Date().toISOString().split('T')[0];
  let md = `# API 接口速查表

> 🤖 此文件由 \`scripts/doc-updater.mjs\` 自动生成
> 📅 最后更新：${now}
> 📦 扫描目录：\`lims-web/src/main/java/com/lims/web/controller/\`

`;

  // 按 basePath 分组排序
  controllers.sort((a, b) => a.basePath.localeCompare(b.basePath));

  let idx = 1;
  for (const ctrl of controllers) {
    md += `## ${idx}. ${ctrl.tagName}\n\n`;
    md += `**Base**: \`${ctrl.basePath}\`\n\n`;
    if (ctrl.tagDesc) {
      md += `> ${ctrl.tagDesc}\n\n`;
    }

    md += '| 方法 | 路径 | 说明 |\n';
    md += '|------|------|------|\n';

    // 按 HTTP 方法排序
    const sorted = [...ctrl.endpoints].sort((a, b) => {
      const oa = METHOD_ORDER.indexOf(a.method);
      const ob = METHOD_ORDER.indexOf(b.method);
      if (oa === ob) return a.path.localeCompare(b.path);
      return oa - ob;
    });

    for (const ep of sorted) {
      const fullPath = ep.path ? `${ctrl.basePath}${ep.path}` : ctrl.basePath;
      md += `| **${ep.method}** | \`${fullPath}\` | ${ep.summary} |\n`;
    }

    md += '\n';
    idx++;
  }

  // 统计
  const totalEndpoints = controllers.reduce((sum, c) => sum + c.endpoints.length, 0);
  md += `---\n\n`;
  md += `📊 **统计**: ${controllers.length} 个 Controller · ${totalEndpoints} 个端点\n`;

  return md;
}

// ─── Controller → README 模块映射 ──────────────────────────
// 定义每个 Controller basePath 对应的 README 模块信息
const MODULE_MAP = [
  { basePath: '/api/v1/auth',             name: '🔐 认证与授权', desc: 'Azure AD SSO + JWT + RBAC（Admin/Manager/Engineer/Technician/Requester）' },
  { basePath: '/api/v1/requests',         name: '📋 委托管理',   desc: '实验委托创建 → 审批 → 分配 → 接样 → 报告 → 完成，完整工作流' },
  { basePath: '/api/v1/reports',          name: '📊 报告管理',   desc: 'Word 在线编辑（M365 Graph API）、多版本修订链、审批流' },
  { basePath: '/api/v1/analysis-items',   name: '🧪 测试数据',   desc: '检测项目、分析类型、规格限度、测试组、测试点位 全 CRUD' },
  { basePath: '/api/v1/brands',           name: '🏗️ 基础数据',   desc: '品牌、委托类型、部门树、节假日（含工作日计算）' },
  { basePath: '/api/v1/departments',      name: null,             desc: null },  // 合并到基础数据
  { basePath: '/api/v1/request-types',    name: null,             desc: null },  // 合并到基础数据
  { basePath: '/api/v1/holidays',         name: null,             desc: null },  // 合并到基础数据
  { basePath: '/api/v1/equipments',       name: '🔧 设备管理',   desc: '设备台账 + 维修工单（创建→自动标记维修中→完成恢复）' },
  { basePath: '/api/v1/equipment-repairs',name: null,             desc: null },  // 合并到设备管理
  { basePath: '/api/v1/knowledge-docs',   name: '📚 知识库',     desc: '文档上传/下载/分类检索（MinIO 存储）' },
  { basePath: '/api/v1/dashboard',        name: '📈 仪表盘',     desc: '任务看板、经理概览、委托统计、费用统计与导出' },
  { basePath: '/api/v1/admin',            name: '📝 审计日志',   desc: '全量操作记录，含请求参数 JSON 详情' },
  { basePath: '/api/v1/i18n',             name: '🌍 国际化',     desc: 'i18n 字典管理，中英双语' },
  { basePath: '/api/v1/sync',             name: '🔄 数据同步',   desc: '用户/部门从外部系统批量同步' },
  { basePath: '/api/v1/external',         name: '🔗 外部集成',   desc: '零部件/供应商主数据查询，计费数据导出' },
];

// 将 MODULE_MAP 中 desc 为 null 的条目合并到前一个拥有独立 name 的模块
function buildModuleTable(controllers) {
  const controllerBasePaths = new Set(controllers.map(c => c.basePath));
  const modules = [];
  let currentModule = null;

  for (const entry of MODULE_MAP) {
    if (entry.name !== null) {
      // 新模块开始
      if (currentModule) modules.push(currentModule);
      currentModule = {
        name: entry.name,
        desc: entry.desc,
        basePaths: [entry.basePath],
        active: controllerBasePaths.has(entry.basePath),
      };
    } else if (currentModule) {
      // 合并到当前模块
      currentModule.basePaths.push(entry.basePath);
      if (controllerBasePaths.has(entry.basePath)) {
        currentModule.active = true;
      }
    }
  }
  if (currentModule) modules.push(currentModule);

  // 检查是否有未映射的 Controller
  const mappedPaths = new Set(MODULE_MAP.map(e => e.basePath));
  const unmapped = [...controllerBasePaths].filter(p => !mappedPaths.has(p));
  if (unmapped.length > 0) {
    modules.push({
      name: '❓ 未分类',
      desc: '以下端点尚未在 MODULE_MAP 中注册',
      basePaths: unmapped,
      active: true,
    });
  }

  return modules;
}

// ─── 同步 README 功能模块表 ─────────────────────────────────
function syncReadmeModules(controllers, dryRun) {
  if (!existsSync(README_PATH)) {
    console.log('⚠️  README.md 不存在，跳过同步');
    return;
  }

  const readme = readFileSync(README_PATH, 'utf-8');
  const modules = buildModuleTable(controllers);

  // 生成新的模块表格
  const rows = modules.map(m => {
    const status = m.active ? '✅' : '⚠️';
    return `| ${m.name} | ${m.desc} | ${status} |`;
  });

  const newTable = [
    '| 模块 | 描述 | 状态 |',
    '|------|------|------|',
    ...rows,
    '',
  ].join('\n');

  // 匹配现有的功能模块表格区域（从 ## 功能模块 到下一个 ## 之间）
  const sectionRegex = /(## 功能模块\s*\n\n)(\| 模块[^#]*?)(\n\n## )/s;
  const match = readme.match(sectionRegex);

  if (!match) {
    console.log('⚠️  无法在 README.md 中定位「功能模块」表格区域');
    return;
  }

  const newContent = readme.replace(
    sectionRegex,
    `$1${newTable}$3`
  );

  if (dryRun) {
    console.log('\n📝 [DRY RUN] README.md 功能模块表将更新为:\n');
    console.log(newTable);
    // 对比差异
    const oldTable = match[2];
    if (oldTable.trim() !== newTable.trim()) {
      console.log('🔀 检测到差异（旧 → 新）:');
      // 简化对比：比较行数
      const oldLines = oldTable.trim().split('\n').filter(l => l.startsWith('|'));
      const newLines = newTable.trim().split('\n').filter(l => l.startsWith('|'));
      console.log(`   旧表格: ${oldLines.length - 1} 行模块`);
      console.log(`   新表格: ${newLines.length - 1} 行模块`);
    } else {
      console.log('   (无变化)');
    }
  } else {
    writeFileSync(README_PATH, newContent, 'utf-8');
    console.log(`✅ 已同步 README.md 功能模块表 (${modules.length} 个模块)`);
  }

  // 汇报未映射的 Controller
  const unmappedModule = modules.find(m => m.name === '❓ 未分类');
  if (unmappedModule) {
    console.log(`\n⚠️  以下 Controller basePath 未在 MODULE_MAP 中注册:`);
    unmappedModule.basePaths.forEach(p => console.log(`   - ${p}`));
    console.log('   请在 scripts/doc-updater.mjs 的 MODULE_MAP 中添加映射');
  }
}

// ─── 主流程 ─────────────────────────────────────────────────
function main() {
  const args = parseArgs();

  if (args.help) {
    console.log(`
🧢 Stacky's doc-updater — Material LIMS 文档自动更新器

用法:
  node scripts/doc-updater.mjs                   更新 api-summary.md
  node scripts/doc-updater.mjs --dry-run          预览模式，不写入文件
  node scripts/doc-updater.mjs --check-readme     检查 README.md 模块一致性
  node scripts/doc-updater.mjs --sync-readme      自动同步 README.md 功能模块表
  node scripts/doc-updater.mjs --help             显示此帮助

原理:
  扫描所有 Controller.java → 解析 Spring MVC 注解 → 生成 Markdown
  --sync-readme: 根据 Controller 扫描结果自动更新 README 功能模块表
`);
    return;
  }

  console.log('🔍 扫描 Controller 文件...\n');
  const controllers = scanControllers();

  if (controllers.length === 0) {
    console.log('⚠️  未找到任何 Controller');
    return;
  }

  console.log(`📋 找到 ${controllers.length} 个 Controller:\n`);
  for (const c of controllers) {
    console.log(`   ${c.tagName.padEnd(30)} ${c.basePath}  (${c.endpoints.length} 端点)`);
  }

  const markdown = generateApiSummary(controllers);

  if (args.dryRun) {
    console.log('\n📝 [DRY RUN] 预览 api-summary.md:\n');
    console.log(markdown);
  } else {
    const dir = dirname(API_SUMMARY_PATH);
    if (!existsSync(dir)) mkdirSync(dir, { recursive: true });
    writeFileSync(API_SUMMARY_PATH, markdown, 'utf-8');
    console.log(`\n✅ 已写入: ${API_SUMMARY_PATH}`);
  }

  if (args.checkReadme) {
    checkReadmeModules(controllers);
  }

  if (args.syncReadme) {
    syncReadmeModules(controllers, args.dryRun);
  }
}

main();
