import { defineConfig } from '@playwright/test';

/**
 * Material-LIMS 主 E2E 测试配置 ★
 *
 * ═══════════════════════════════════════════════════════════════
 * 这是 CI 流水线使用的主测试配置。
 *
 * 策略：API 级端到端测试（dev 模式下 DevAuthFilter 自动注入 ADMIN 用户）
 * 所有测试通过 HTTP 请求直接调用 REST API，覆盖完整业务流程。
 *
 * 另有一套浏览器级 UI 测试配置位于：../../playwright.config.ts
 * 用于需要真实浏览器交互的场景（登录、页面导航等）。
 *
 * 运行：
 *   cd lims-web-ui && npx playwright test         # 运行测试
 *   cd lims-web-ui && npx playwright test --ui    # UI 模式调试
 *   cd lims-web-ui && npx playwright show-report  # 查看报告
 * ═══════════════════════════════════════════════════════════════
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,           // API 测试串行执行，避免数据竞态
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,                     // 单 worker，保证测试数据隔离
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['junit', { outputFile: 'test-results/e2e-junit.xml' }],
    ['list'],
  ],
  timeout: 30000,                 // 单测 30s 超时
  expect: {
    timeout: 10000,
  },
  use: {
    baseURL: 'http://localhost:8080',
    extraHTTPHeaders: {
      'Content-Type': 'application/json',
    },
    // 截图 / 录像 / trace 策略
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'on-first-retry',
  },
  // 输出目录
  outputDir: 'test-results/',
});
