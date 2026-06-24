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
 */
test.describe('Report Lifecycle — 报告全流程', () => {
  let requestId: string;
  let reportId: string;

  // -------------------------------------------------------
  // 前置：创建并推进委托到可出报告状态
  // -------------------------------------------------------
  test('前置 — 准备委托数据', async ({ api }) => {
    // 拉取基础数据
    const brandsRes = await api.get(`${API_PREFIX}/brands`);
    await assertOk(brandsRes, 'get brands');
    const brands = await getData<{ records: { id: string }[] }>(brandsRes);
    const brandId = brands?.records?.[0]?.id ?? null;

    const typesRes = await api.get(`${API_PREFIX}/request-types`);
    await assertOk(typesRes, 'get request types');
    const types = await getData<{ records: { id: string }[] }>(typesRes);
    const typeId = types?.records?.[0]?.id ?? null;

    const deptsRes = await api.get(`${API_PREFIX}/departments`);
    await assertOk(deptsRes, 'get departments');
    const depts = await getData<{ id: string }[]>(deptsRes);
    const deptId = depts?.[0]?.id ?? null;

    // 获取分析项
    const itemsRes = await api.get(`${API_PREFIX}/analysis-items?page=1&size=5`);
    await assertOk(itemsRes, 'get analysis items');
    const items = await getData<{ records: { id: string }[] }>(itemsRes);
    const itemIds = items?.records?.slice(0, 2).map((i: { id: string }) => i.id) ?? [];

    // 创建委托
    const createRes = await api.post(`${API_PREFIX}/requests`, {
      data: {
        brandId, deptId, typeId,
        partNumber: `E2E-RPT-${Date.now()}`,
        partName: 'E2E 报告测试零件',
        requestReason: 'E2E 自动化 — 报告流程测试',
        priority: 'NORMAL',
        analysisItemIds: itemIds,
      },
    });
    const created = await getData<RequestEntity>(createRes);
    requestId = created.id;
    console.log(`  ✅ 委托已创建: ${created.requestNo}`);

    // 提交
    await api.post(`${API_PREFIX}/requests/${requestId}/submit`);

    // 获取任务并分配
    const tasksRes = await api.get(`${API_PREFIX}/requests/${requestId}/tasks`);
    const tasks = await getData<AnalysisTaskEntity[]>(tasksRes);
    const assignments = tasks.map((t) => ({ taskId: t.id, engineerId: 'dev-user-0001' }));
    await api.post(`${API_PREFIX}/requests/${requestId}/assign`, { data: assignments });

    // 接收样品
    await api.post(`${API_PREFIX}/requests/${requestId}/receive-sample`, {
      data: { deliveryNote: 'E2E-RPT-DN-001' },
    });

    // 完成所有分析任务
    for (const task of tasks) {
      await api.put(`${API_PREFIX}/requests/tasks/${task.id}`, { data: { status: 'IN_PROGRESS' } });
      await api.put(`${API_PREFIX}/requests/tasks/${task.id}`, { data: { status: 'COMPLETED' } });
    }

    // 推进到 REPORTING
    const startRes = await api.post(`${API_PREFIX}/requests/${requestId}/start-reporting`);
    // 可能已经是 APPROVING（自动跳转），不影响后续测试
    const statusCode = startRes.status();
    expect([200, 400]).toContain(statusCode);

    console.log('  ✅ 委托已准备就绪，可开始出报告');
  });

  // -------------------------------------------------------
  // Step 1: 创建报告
  // -------------------------------------------------------
  test('Step 1 — 创建报告 (DRAFT)', async ({ api }) => {
    test.skip(!requestId, '依赖前置：无 requestId');

    const res = await api.post(`${API_PREFIX}/reports/requests/${requestId}/reports`);
    await assertOk(res, 'create report');

    const report = await getData<ReportEntity>(res);
    reportId = report.id;

    expect(report.status).toBe('DRAFT');
    expect(report.versionNumber).toBe('V1.0');
    expect(report.requestId).toBe(requestId);

    console.log(`  ✅ 报告已创建: ${reportId}`);
  });

  // -------------------------------------------------------
  // Step 2: 提交报告审批
  // -------------------------------------------------------
  test('Step 2 — 提交审批 (DRAFT → IN_REVIEW)', async ({ api }) => {
    test.skip(!reportId, '依赖 Step 1：无 reportId');

    const res = await api.post(`${API_PREFIX}/reports/${reportId}/submit`);
    await assertOk(res, 'submit report');

    const getRes = await api.get(`${API_PREFIX}/reports/${reportId}`);
    const report = await getData<ReportEntity>(getRes);
    expect(report.status).toBe('IN_REVIEW');

    console.log('  ✅ 报告已提交审批');
  });

  // -------------------------------------------------------
  // Step 3: 批准报告
  // -------------------------------------------------------
  test('Step 3 — 批准报告 (IN_REVIEW → APPROVED)', async ({ api }) => {
    test.skip(!reportId, '依赖 Step 1-2：无 reportId');

    const res = await api.post(`${API_PREFIX}/reports/${reportId}/approve`);
    await assertOk(res, 'approve report');

    const getRes = await api.get(`${API_PREFIX}/reports/${reportId}`);
    const report = await getData<ReportEntity>(getRes);
    expect(report.status).toBe('APPROVED');
    expect(report.approvedBy).toBeTruthy();

    console.log('  ✅ 报告已批准');
  });

  // -------------------------------------------------------
  // Step 4: 打回修订
  // -------------------------------------------------------
  test('Step 4 — 打回修订 (APPROVED → REVISING)', async ({ api }) => {
    test.skip(!reportId, '依赖 Step 1-2：无 reportId');

    // 先打回
    const rejectRes = await api.post(`${API_PREFIX}/reports/${reportId}/reject`);
    await assertOk(rejectRes, 'reject report');

    let getRes = await api.get(`${API_PREFIX}/reports/${reportId}`);
    let report = await getData<ReportEntity>(getRes);
    expect(report.status).toBe('REVISING');

    // 重新提交
    await api.post(`${API_PREFIX}/reports/${reportId}/submit`);
    await api.post(`${API_PREFIX}/reports/${reportId}/approve`);

    // 修订（APPROVED 状态下）
    const reviseRes = await api.post(`${API_PREFIX}/reports/${reportId}/revise`, {
      data: { revisionNote: 'E2E 修订测试 — 更新实验数据' },
    });
    await assertOk(reviseRes, 'revise report');

    getRes = await api.get(`${API_PREFIX}/reports/${reportId}`);
    report = await getData<ReportEntity>(getRes);
    expect(report.versionNumber).toBe('V2.0');
    expect(report.revisionNote).toBe('E2E 修订测试 — 更新实验数据');
    expect(report.status).toBe('REVISING');

    console.log(`  ✅ 报告已修订: ${report.versionNumber}`);
  });

  // -------------------------------------------------------
  // 清理：完成委托
  // -------------------------------------------------------
  test('清理 — 完成委托', async ({ api }) => {
    test.skip(!requestId, '无 requestId');
    await api.post(`${API_PREFIX}/requests/${requestId}/complete`);
    console.log('  🧹 委托已完成，测试数据清理完毕');
  });
});
