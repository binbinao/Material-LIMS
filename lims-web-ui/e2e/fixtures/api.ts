import { test as base, APIRequestContext, APIResponse, type Playwright } from '@playwright/test';

/**
 * Material-LIMS E2E 测试 Fixtures
 *
 * dev 模式下 DevAuthFilter 自动注入 dev-user-0001（ALL ROLES），
 * 无需处理登录 / Token，直接调用 API 即可。
 */

export const API_PREFIX = '/api/v1';

/** 默认测试用户 ID（与 DevAuthFilter.DEV_USER_ID 一致） */
export const TEST_USER_ID = 'dev-user-0001';

// ============================================================
// 类型定义
// ============================================================

export interface CreateRequestPayload {
  brandId?: string;
  deptId?: string;
  typeId?: string;
  partNumber?: string;
  partName?: string;
  eco?: string;
  supplierCode?: string;
  supplierName?: string;
  requestReason?: string;
  priority?: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  analysisItemIds?: string[];
  proxyRequest?: boolean;
  realRequesterName?: string;
}

export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

export interface RequestEntity {
  id: string;
  requestNo: string;
  status: string;
  brandId?: string;
  deptId?: string;
  typeId?: string;
  requesterId?: string;
  partNumber?: string;
  partName?: string;
  priority?: string;
  dueDate?: string;
  processInstanceId?: string;
  totalCost?: number;
}

export interface AnalysisTaskEntity {
  id: string;
  requestId: string;
  itemId: string;
  status: string;
  assigneeId?: string;
  sortOrder?: number;
  delayReason?: string;
}

export interface ReportEntity {
  id: string;
  requestId: string;
  authorId: string;
  versionNumber: string;
  status: string;
  fileUrl?: string;
  pdfUrl?: string;
  approvedBy?: string;
  revisionNote?: string;
}

export interface EquipmentEntity {
  id: string;
  name: string;
  model?: string;
  serialNumber?: string;
  status?: string;
}

export interface BrandEntity {
  id: string;
  name: string;
}

// ============================================================
// Test Fixtures
// ============================================================

type ApiFixtures = {
  api: APIRequestContext;
  /** ENGINEER 角色 API（用于创建报告等操作） */
  engineerApi: APIRequestContext;
  /** MANAGER 角色 API（用于审批等需要四眼原则的操作） */
  managerApi: APIRequestContext;
};

function createApiContext(playwright: Playwright, devUser: string): Promise<APIRequestContext> {
  return playwright.request.newContext({
    baseURL: 'http://localhost:8080',
    extraHTTPHeaders: {
      'Content-Type': 'application/json',
      'X-Dev-User': devUser,
    },
  });
}

/**
 * 扩展基础 test，注入多角色 API request context
 */
export const test = base.extend<ApiFixtures>({
  api: async ({ playwright }, use) => {
    const ctx = await createApiContext(playwright, '');
    await use(ctx);
    await ctx.dispose();
  },
  engineerApi: async ({ playwright }, use) => {
    const ctx = await createApiContext(playwright, 'engineer');
    await use(ctx);
    await ctx.dispose();
  },
  managerApi: async ({ playwright }, use) => {
    const ctx = await createApiContext(playwright, 'manager');
    await use(ctx);
    await ctx.dispose();
  },
});

export { expect } from '@playwright/test';

// ============================================================
// 辅助函数
// ============================================================

/** 检查 API 响应是否成功 */
export async function assertOk(res: APIResponse, context?: string): Promise<void> {
  const label = context ? `[${context}] ` : '';
  if (!res.ok()) {
    const body = await res.text();
    throw new Error(`${label}HTTP ${res.status()}: ${body}`);
  }
}

/** 从响应中提取 data */
export async function getData<T = unknown>(res: APIResponse): Promise<T> {
  const json: ApiResponse<T> = await res.json();
  if (json.code !== 200) {
    throw new Error(`API error code=${json.code}: ${json.message}`);
  }
  return json.data;
}

/** 等待指定毫秒 */
export const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/**
 * 轮询等待实体状态变更
 *
 * 适用场景：Flowable 工作流推进等异步操作，状态切换可能存在延迟。
 * 避免硬编码 sleep，改为条件轮询，超时后抛出明确错误。
 *
 * @param api         - Playwright API request context
 * @param url         - 要轮询的 GET 端点（如 `/api/v1/requests/${id}`）
 * @param expectedStatus - 期望的状态值
 * @param maxRetries  - 最大重试次数（默认 10）
 * @param intervalMs  - 每次轮询间隔（默认 1000ms）
 * @returns 实体数据（达到期望状态时）
 */
export async function waitForStatus<T extends { status: string }>(
  api: APIRequestContext,
  url: string,
  expectedStatus: string,
  maxRetries: number = 10,
  intervalMs: number = 1000,
): Promise<T> {
  for (let i = 0; i < maxRetries; i++) {
    const res = await api.get(url);
    await assertOk(res, `waitForStatus(${expectedStatus}) attempt ${i + 1}`);
    const data = await getData<T>(res);
    if (data.status === expectedStatus) {
      return data;
    }
    if (i < maxRetries - 1) {
      await sleep(intervalMs);
    }
  }
  throw new Error(
    `waitForStatus timed out after ${maxRetries} attempts: ` +
    `expected status "${expectedStatus}" on ${url}`
  );
}
