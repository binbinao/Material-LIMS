import { test as base, APIRequestContext, APIResponse } from '@playwright/test';

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
};

/**
 * 扩展基础 test，注入 API request context
 */
export const test = base.extend<ApiFixtures>({
  api: async ({ playwright }, use) => {
    const apiContext = await playwright.request.newContext({
      baseURL: 'http://localhost:8080',
      extraHTTPHeaders: {
        'Content-Type': 'application/json',
      },
    });
    await use(apiContext);
    await apiContext.dispose();
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
