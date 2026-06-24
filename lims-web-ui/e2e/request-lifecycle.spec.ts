import { test, expect } from './fixtures/api';
import {
  API_PREFIX,
  assertOk,
  getData,
  type RequestEntity,
  type AnalysisTaskEntity,
} from './fixtures/api';

/**
 * 🔴 高优先级 — 委托管理全流程 E2E 测试
 *
 * 覆盖：CREATE → SUBMIT → ASSIGN → RECEIVE SAMPLE → START REPORTING → COMPLETE
 * 这是 Material-LIMS 最核心的业务链路，任何一个环节断裂都影响交付。
 */
test.describe('Request Lifecycle — 委托全流程', () => {
  let requestId: string;
  let taskId: string;

  // -------------------------------------------------------
  // Step 1: 创建委托
  // -------------------------------------------------------
  test('Step 1 — 创建委托 (DRAFT)', async ({ api }) => {
    // 先拉取必要的基础数据
    const brandsRes = await api.get(`${API_PREFIX}/brands`);
    await assertOk(brandsRes, 'get brands');
    const brands = await getData<{ records: unknown[] }>(brandsRes);
    const brandId = brands?.records?.[0] ? (brands.records[0] as { id: string }).id : null;

    const typesRes = await api.get(`${API_PREFIX}/request-types`);
    await assertOk(typesRes, 'get request types');
    const types = await getData<{ records: unknown[] }>(typesRes);
    const typeId = types?.records?.[0] ? (types.records[0] as { id: string }).id : null;

    const deptsRes = await api.get(`${API_PREFIX}/departments`);
    await assertOk(deptsRes, 'get departments');
    const depts = await getData<unknown[]>(deptsRes);
    const deptId = Array.isArray(depts) && depts.length > 0
      ? (depts[0] as { id: string }).id : null;

    // 获取分析项（必填字段）
    const itemsRes = await api.get(`${API_PREFIX}/analysis-items?page=1&size=5`);
    await assertOk(itemsRes, 'get analysis items');
    const items = await getData<{ records: { id: string }[] }>(itemsRes);
    const itemIds = items?.records?.slice(0, 2).map((i: { id: string }) => i.id) ?? [];

    const res = await api.post(`${API_PREFIX}/requests`, {
      data: {
        brandId,
        deptId,
        typeId,
        partNumber: `E2E-PART-${Date.now()}`,
        partName: 'E2E 测试零件',
        eco: 'ECO-2026-001',
        supplierCode: 'SUP-001',
        supplierName: 'E2E 测试供应商',
        requestReason: 'E2E 自动化测试 — 委托创建',
        priority: 'HIGH',
        analysisItemIds: itemIds,
      },
    });

    await assertOk(res, 'create request');
    const created = await getData<RequestEntity>(res);
    requestId = created.id;

    expect(created.requestNo).toMatch(/^REQ-\d{4}-\d{4}$/);
    expect(created.status).toBe('DRAFT');
    expect(created.priority).toBe('HIGH');
    expect(created.partName).toBe('E2E 测试零件');

    console.log(`  ✅ 委托已创建: ${created.requestNo} (${created.id})`);
  });

  // -------------------------------------------------------
  // Step 2: 提交委托 → 启动 Flowable 工作流
  // -------------------------------------------------------
  test('Step 2 — 提交委托 (DRAFT → SUBMITTED)', async ({ api }) => {
    test.skip(!requestId, '依赖 Step 1：无 requestId');

    const res = await api.post(`${API_PREFIX}/requests/${requestId}/submit`);
    await assertOk(res, 'submit request');

    // 验证状态已变更
    const getRes = await api.get(`${API_PREFIX}/requests/${requestId}`);
    const request = await getData<RequestEntity>(getRes);
    expect(request.status).toBe('SUBMITTED');
    expect(request.processInstanceId).toBeTruthy();

    console.log(`  ✅ 委托已提交，工作流已启动: ${request.processInstanceId}`);
  });

  // -------------------------------------------------------
  // Step 3: 经理分配工程师
  // -------------------------------------------------------
  test('Step 3 — 分配工程师 (SUBMITTED → ASSIGNED)', async ({ api }) => {
    test.skip(!requestId, '依赖 Step 1-2：无 requestId');

    // 获取分析任务列表
    const tasksRes = await api.get(`${API_PREFIX}/requests/${requestId}/tasks`);
    await assertOk(tasksRes, 'get tasks');
    const tasks = await getData<AnalysisTaskEntity[]>(tasksRes);
    expect(tasks.length).toBeGreaterThan(0);

    // 选第一个任务，分配给自己（dev 用户有所有角色）
    taskId = tasks[0].id;
    const res = await api.post(`${API_PREFIX}/requests/${requestId}/assign`, {
      data: [{ taskId, engineerId: 'dev-user-0001' }],
    });

    await assertOk(res, 'assign request');

    const getRes = await api.get(`${API_PREFIX}/requests/${requestId}`);
    const request = await getData<RequestEntity>(getRes);
    expect(request.status).toBe('ASSIGNED');

    console.log(`  ✅ 已分配工程师，taskId=${taskId}`);
  });

  // -------------------------------------------------------
  // Step 4: 接收样品
  // -------------------------------------------------------
  test('Step 4 — 接收样品 (ASSIGNED → SAMPLING)', async ({ api }) => {
    test.skip(!requestId, '依赖 Step 1-3：无 requestId');

    const res = await api.post(`${API_PREFIX}/requests/${requestId}/receive-sample`, {
      data: { deliveryNote: 'E2E-DN-2026-001' },
    });

    await assertOk(res, 'receive sample');

    const getRes = await api.get(`${API_PREFIX}/requests/${requestId}`);
    const request = await getData<RequestEntity>(getRes);
    expect(request.status).toBe('SAMPLING');

    console.log('  ✅ 样品已接收');
  });

  // -------------------------------------------------------
  // Step 5: 开始出报告
  // -------------------------------------------------------
  test('Step 5 — 开始出报告 (SAMPLING → REPORTING)', async ({ api }) => {
    test.skip(!requestId, '依赖 Step 1-4：无 requestId');

    // 先把分析任务标记为完成
    const tasksRes = await api.get(`${API_PREFIX}/requests/${requestId}/tasks`);
    const tasks = await getData<AnalysisTaskEntity[]>(tasksRes);
    for (const task of tasks) {
      await api.put(`${API_PREFIX}/requests/tasks/${task.id}`, {
        data: { status: 'IN_PROGRESS' },
      });
      await api.put(`${API_PREFIX}/requests/tasks/${task.id}`, {
        data: { status: 'COMPLETED' },
      });
    }

    // 推进到 REPORTING
    const res = await api.post(`${API_PREFIX}/requests/${requestId}/start-reporting`);
    await assertOk(res, 'start reporting');

    const getRes = await api.get(`${API_PREFIX}/requests/${requestId}`);
    const request = await getData<RequestEntity>(getRes);
    // REPORTING 或 APPROVING（所有任务完成后自动跳转）
    expect(['REPORTING', 'APPROVING']).toContain(request.status);

    console.log(`  ✅ 报告阶段已启动，当前状态: ${request.status}`);
  });

  // -------------------------------------------------------
  // Step 6: 完成委托
  // -------------------------------------------------------
  test('Step 6 — 完成委托 (→ COMPLETED)', async ({ api }) => {
    test.skip(!requestId, '依赖 Step 1-5：无 requestId');

    const res = await api.post(`${API_PREFIX}/requests/${requestId}/complete`);
    await assertOk(res, 'complete request');

    const getRes = await api.get(`${API_PREFIX}/requests/${requestId}`);
    const request = await getData<RequestEntity>(getRes);
    expect(request.status).toBe('COMPLETED');

    console.log(`  ✅ 委托已完成: ${request.requestNo}`);
  });
});
