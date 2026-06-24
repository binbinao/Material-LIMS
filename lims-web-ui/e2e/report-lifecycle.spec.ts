import { test, expect } from './fixtures/api';
import {
  API_PREFIX,
  assertOk,
  getData,
  type RequestEntity,
  type ReportEntity,
  type AnalysisTaskEntity,
} from './fixtures/api';

/**
 * 🔴 高优先级 — 报告管理全流程 E2E 测试
 *
 * 覆盖：创建委托 → 提交 → 分配 → 完成分析 → 出报告 → 提交审批 → 批准 → 修订
 *
 * 设计决策：所有步骤（含前置准备和清理）合并为一个 test，
 * 消除跨 test 共享变量（requestId / reportId）的竞态和调试成本。
 */
test.describe('Report Lifecycle — 报告全流程', () => {
  test('全流程 — 准备委托 → 创建报告 → 审批 → 修订 → 清理', async ({ api, engineerApi, managerApi }) => {
    // ================================================================
    // 0. 拉取基础数据
    // ================================================================
    const [brandsRes, typesRes, deptsRes, itemsRes] = await Promise.all([
      api.get(`${API_PREFIX}/brands`),
      api.get(`${API_PREFIX}/request-types`),
      api.get(`${API_PREFIX}/departments`),
      api.get(`${API_PREFIX}/analysis-items?page=1&size=5`),
    ]);

    const brands = await getData<{ records: { id: string }[] }>(brandsRes);
    const types = await getData<{ records: { id: string }[] }>(typesRes);
    const depts = await getData<{ id: string }[]>(deptsRes);
    const items = await getData<{ records: { id: string }[] }>(itemsRes);

    const brandId = brands?.records?.[0]?.id ?? null;
    const typeId = types?.records?.[0]?.id ?? null;
    const deptId = depts?.[0]?.id ?? null;
    const itemIds = items?.records?.slice(0, 2).map(i => i.id) ?? [];

    // ================================================================
    // 前置：创建委托并推进到可出报告状态（用全能 dev 用户）
    // ================================================================
    const timestamp = Date.now();
    const createReqRes = await api.post(`${API_PREFIX}/requests`, {
      data: {
        brandId, deptId, typeId,
        partNumber: `E2E-RPT-${timestamp}`,
        partName: 'E2E 报告测试零件',
        requestReason: 'E2E 自动化 — 报告流程测试',
        priority: 'NORMAL',
        analysisItemIds: itemIds,
      },
    });
    const created = await getData<RequestEntity>(createReqRes);
    const requestId = created.id;
    console.log(`  ✅ 委托已创建: ${created.requestNo}`);

    await assertOk(
      await api.post(`${API_PREFIX}/requests/${requestId}/submit`),
      '前置: submit request'
    );

    const tasksRes = await api.get(`${API_PREFIX}/requests/${requestId}/tasks`);
    const tasks = await getData<AnalysisTaskEntity[]>(tasksRes);
    const assignments = tasks.map(t => ({ taskId: t.id, engineerId: 'dev-user-0001' }));
    await api.post(`${API_PREFIX}/requests/${requestId}/assign`, { data: assignments });

    await api.post(`${API_PREFIX}/requests/${requestId}/receive-sample`, {
      data: { deliveryNote: 'E2E-RPT-DN-001' },
    });

    for (const task of tasks) {
      await api.put(`${API_PREFIX}/requests/tasks/${task.id}`, { data: { status: 'IN_PROGRESS' } });
      await api.put(`${API_PREFIX}/requests/tasks/${task.id}`, { data: { status: 'COMPLETED' } });
    }

    const startStatus = (await api.post(`${API_PREFIX}/requests/${requestId}/start-reporting`)).status();
    expect([200, 400]).toContain(startStatus);
    console.log('  ✅ 前置: 委托已准备就绪');

    // ================================================================
    // Step 1: 创建报告 — 用 ENGINEER 身份（author = user-engineer-001）
    // ================================================================
    const reportRes = await engineerApi.post(`${API_PREFIX}/reports/requests/${requestId}/reports`);
    await assertOk(reportRes, 'Step 1: create report');

    const report = await getData<ReportEntity>(reportRes);
    const reportId = report.id;
    expect(report.status).toBe('DRAFT');
    expect(report.versionNumber).toBe('V1.0');
    expect(report.requestId).toBe(requestId);
    console.log(`  ✅ Step 1: 报告已创建 [author=engineer]`);

    // ================================================================
    // Step 2: 提交审批 — ENGINEER 提交 (DRAFT → IN_REVIEW)
    // ================================================================
    await assertOk(
      await engineerApi.post(`${API_PREFIX}/reports/${reportId}/submit`),
      'Step 2: submit report'
    );
    const inReview = await getData<ReportEntity>(
      await api.get(`${API_PREFIX}/reports/${reportId}`)
    );
    expect(inReview.status).toBe('IN_REVIEW');
    console.log('  ✅ Step 2: 报告已提交审批');

    // ================================================================
    // Step 3: 批准报告 — MANAGER 审批 (IN_REVIEW → APPROVED)
    // 四眼原则：manager ≠ engineer，满足 authorId ≠ approverId
    // ================================================================
    await assertOk(
      await managerApi.post(`${API_PREFIX}/reports/${reportId}/approve`),
      'Step 3: approve report'
    );
    const approved = await getData<ReportEntity>(
      await api.get(`${API_PREFIX}/reports/${reportId}`)
    );
    expect(approved.status).toBe('APPROVED');
    expect(approved.approvedBy).toBeTruthy();
    console.log('  ✅ Step 3: 报告已批准 [approver=manager]');

    // ================================================================
    // Step 4: 打回 → 修订 (APPROVED → REVISING → V2.0)
    // ================================================================
    // 4a. MANAGER 打回
    await assertOk(
      await managerApi.post(`${API_PREFIX}/reports/${reportId}/reject`),
      'Step 4a: reject report'
    );
    let current = await getData<ReportEntity>(
      await api.get(`${API_PREFIX}/reports/${reportId}`)
    );
    expect(current.status).toBe('REVISING');

    // 4b. ENGINEER 重新提交 + MANAGER 批准
    await engineerApi.post(`${API_PREFIX}/reports/${reportId}/submit`);
    await managerApi.post(`${API_PREFIX}/reports/${reportId}/approve`);

    // 4c. ENGINEER 修订（APPROVED 状态下触发版本升级）
    await assertOk(
      await engineerApi.post(`${API_PREFIX}/reports/${reportId}/revise`, {
        data: { revisionNote: 'E2E 修订测试 — 更新实验数据' },
      }),
      'Step 4c: revise report'
    );
    current = await getData<ReportEntity>(
      await api.get(`${API_PREFIX}/reports/${reportId}`)
    );
    expect(current.versionNumber).toBe('V2.0');
    expect(current.revisionNote).toBe('E2E 修订测试 — 更新实验数据');
    expect(current.status).toBe('REVISING');
    console.log(`  ✅ Step 4: 报告已修订 [${current.versionNumber}]`);

    // ================================================================
    // 清理：完成委托
    // ================================================================
    await api.post(`${API_PREFIX}/requests/${requestId}/complete`);
    console.log('  🧹 清理: 委托已完成');
  });
});
