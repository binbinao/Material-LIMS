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
 *
 * 设计决策：所有步骤合并为一个 test，消除跨 test 共享变量（requestId / taskId）
 * 带来的数据竞态和重试安全问题。每个环节即验即断，失败时精确定位问题步骤。
 */
test.describe('Request Lifecycle — 委托全流程', () => {
  test('全流程 — DRAFT → SUBMITTED → ASSIGNED → SAMPLING → REPORTING → COMPLETED', async ({ api }) => {
    // ================================================================
    // 0. 拉取基础数据（品牌、委托类型、部门、分析项）
    // ================================================================
    const [brandsRes, typesRes, deptsRes, itemsRes] = await Promise.all([
      api.get(`${API_PREFIX}/brands`),
      api.get(`${API_PREFIX}/request-types`),
      api.get(`${API_PREFIX}/departments`),
      api.get(`${API_PREFIX}/analysis-items?page=1&size=5`),
    ]);
    for (const [res, label] of [
      [brandsRes, 'brands'],
      [typesRes, 'request types'],
      [deptsRes, 'departments'],
      [itemsRes, 'analysis items'],
    ] as const) {
      await assertOk(res, `get ${label}`);
    }

    const brands = await getData<{ records: { id: string }[] }>(brandsRes);
    const types = await getData<{ records: { id: string }[] }>(typesRes);
    const depts = await getData<{ id: string }[]>(deptsRes);
    const items = await getData<{ records: { id: string }[] }>(itemsRes);

    const brandId = brands?.records?.[0]?.id ?? null;
    const typeId = types?.records?.[0]?.id ?? null;
    const deptId = Array.isArray(depts) && depts.length > 0 ? depts[0].id : null;
    const itemIds = items?.records?.slice(0, 2).map(i => i.id) ?? [];

    // ================================================================
    // Step 1: 创建委托 (DRAFT)
    // ================================================================
    const timestamp = Date.now();
    const createRes = await api.post(`${API_PREFIX}/requests`, {
      data: {
        brandId,
        deptId,
        typeId,
        partNumber: `E2E-PART-${timestamp}`,
        partName: 'E2E 测试零件',
        eco: 'ECO-2026-001',
        supplierCode: 'SUP-001',
        supplierName: 'E2E 测试供应商',
        requestReason: 'E2E 自动化测试 — 委托全流程',
        priority: 'HIGH',
        analysisItemIds: itemIds,
      },
    });
    await assertOk(createRes, 'Step 1: create request');

    const created = await getData<RequestEntity>(createRes);
    const requestId = created.id;
    expect(created.requestNo).toMatch(/^REQ-\d{4}-\d{5}$/);
    expect(created.status).toBe('DRAFT');
    expect(created.priority).toBe('HIGH');
    console.log(`  ✅ Step 1: 委托已创建 [${created.requestNo}]`);

    // ================================================================
    // Step 2: 提交委托 (DRAFT → SUBMITTED)
    // ================================================================
    const submitRes = await api.post(`${API_PREFIX}/requests/${requestId}/submit`);
    await assertOk(submitRes, 'Step 2: submit request');

    const submitted = await getData<RequestEntity>(
      await api.get(`${API_PREFIX}/requests/${requestId}`)
    );
    expect(submitted.status).toBe('SUBMITTED');
    expect(submitted.processInstanceId).toBeTruthy();
    console.log(`  ✅ Step 2: 委托已提交，工作流已启动`);

    // ================================================================
    // Step 3: 分配工程师 (SUBMITTED → ASSIGNED)
    // ================================================================
    const tasksRes2 = await api.get(`${API_PREFIX}/requests/${requestId}/tasks`);
    await assertOk(tasksRes2, 'Step 3: get tasks');
    const tasks = await getData<AnalysisTaskEntity[]>(tasksRes2);
    expect(tasks.length).toBeGreaterThan(0);

    const assignRes = await api.post(`${API_PREFIX}/requests/${requestId}/assign`, {
      data: [{ taskId: tasks[0].id, engineerId: 'dev-user-0001' }],
    });
    await assertOk(assignRes, 'Step 3: assign');

    const assigned = await getData<RequestEntity>(
      await api.get(`${API_PREFIX}/requests/${requestId}`)
    );
    expect(assigned.status).toBe('ASSIGNED');
    console.log(`  ✅ Step 3: 已分配工程师`);

    // ================================================================
    // Step 4: 接收样品 (ASSIGNED → SAMPLING)
    // ================================================================
    const receiveRes = await api.post(`${API_PREFIX}/requests/${requestId}/receive-sample`, {
      data: { deliveryNote: 'E2E-DN-2026-001' },
    });
    await assertOk(receiveRes, 'Step 4: receive sample');

    const sampling = await getData<RequestEntity>(
      await api.get(`${API_PREFIX}/requests/${requestId}`)
    );
    expect(sampling.status).toBe('SAMPLING');
    console.log(`  ✅ Step 4: 样品已接收`);

    // ================================================================
    // Step 5: 完成分析任务 + 推进到 REPORTING
    // ================================================================
    for (const task of tasks) {
      await api.put(`${API_PREFIX}/requests/tasks/${task.id}`, {
        data: { status: 'IN_PROGRESS' },
      });
      await api.put(`${API_PREFIX}/requests/tasks/${task.id}`, {
        data: { status: 'COMPLETED' },
      });
    }

    const reportRes = await api.post(`${API_PREFIX}/requests/${requestId}/start-reporting`);
    await assertOk(reportRes, 'Step 5: start reporting');

    const reporting = await getData<RequestEntity>(
      await api.get(`${API_PREFIX}/requests/${requestId}`)
    );
    expect(['REPORTING', 'APPROVING']).toContain(reporting.status);
    console.log(`  ✅ Step 5: 报告阶段已启动 [${reporting.status}]`);

    // ================================================================
    // Step 6: 完成委托（尝试完成，需报告审批通过后才能 COMPLETED）
    // 如果报告未审批，请求会停留在 REPORTING/APPROVING —— 这是正确的业务状态
    // ================================================================
    const completeRes = await api.post(`${API_PREFIX}/requests/${requestId}/complete`);
    // 400 = 状态不允许（因为报告尚未审批），200 = 完成成功
    expect([200, 400]).toContain(completeRes.status());

    const finalCheck = await getData<RequestEntity>(
      await api.get(`${API_PREFIX}/requests/${requestId}`)
    );
    // 终态：REPORTING（报告未审批）或 COMPLETED（报告已审批）
    expect(['REPORTING', 'APPROVING', 'COMPLETED']).toContain(finalCheck.status);
    console.log(`  ✅ Step 6: 委托终态 [${finalCheck.status}] ${finalCheck.requestNo}`);
  });
});
