import { test, expect } from './fixtures/api';
import {
  API_PREFIX,
  assertOk,
  getData,
  type EquipmentEntity,
} from './fixtures/api';

/**
 * 🟡 中优先级 — 设备管理 E2E 测试
 *
 * 覆盖：CRUD 操作 + 设备维修流程
 */
test.describe('Equipment Management — 设备管理', () => {
  let equipmentId: string;
  let repairId: string;

  // -------------------------------------------------------
  // 设备 CRUD
  // -------------------------------------------------------
  test('CRUD — 创建 → 查询 → 更新 → 删除', async ({ api }) => {
    // CREATE (status 必须匹配 CHECK 约束: ACTIVE/UNDER_REPAIR/DECOMMISSIONED)
    const createRes = await api.post(`${API_PREFIX}/equipments`, {
      data: {
        name: 'E2E 测试设备',
        model: 'X-1000',
        serialNumber: `E2E-SN-${Date.now()}`,
        status: 'ACTIVE',
        location: 'Lab A',
      },
    });
    await assertOk(createRes, 'create equipment');
    const created = await getData<EquipmentEntity>(createRes);
    equipmentId = created.id;
    expect(created.name).toBe('E2E 测试设备');
    expect(created.status).toBe('ACTIVE');

    // READ (list)
    const listRes = await api.get(`${API_PREFIX}/equipments?page=1&size=10`);
    await assertOk(listRes, 'list equipments');
    const list = await getData<{ records: EquipmentEntity[] }>(listRes);
    expect(list.records.some((e) => e.id === equipmentId)).toBe(true);

    // READ (detail)
    const getRes = await api.get(`${API_PREFIX}/equipments/${equipmentId}`);
    await assertOk(getRes, 'get equipment');
    const detail = await getData<EquipmentEntity>(getRes);
    expect(detail.model).toBe('X-1000');

    // UPDATE
    const updateRes = await api.put(`${API_PREFIX}/equipments/${equipmentId}`, {
      data: { name: 'E2E 测试设备 (已更新)', status: 'UNDER_REPAIR' },
    });
    await assertOk(updateRes, 'update equipment');

    const updated = await getData<EquipmentEntity>(
      await api.get(`${API_PREFIX}/equipments/${equipmentId}`)
    );
    expect(updated.name).toBe('E2E 测试设备 (已更新)');
    expect(updated.status).toBe('UNDER_REPAIR');

    // DELETE
    const deleteRes = await api.delete(`${API_PREFIX}/equipments/${equipmentId}`);
    await assertOk(deleteRes, 'delete equipment');

    console.log(`  ✅ 设备 CRUD 全流程通过`);
  });

  // -------------------------------------------------------
  // 设备维修流程
  // -------------------------------------------------------
  test('维修流程 — 创建维修 → 完成维修', async ({ api }) => {
    // 先创建设备
    const createRes = await api.post(`${API_PREFIX}/equipments`, {
      data: {
        name: 'E2E 维修测试设备',
        model: 'R-2000',
        serialNumber: `E2E-REPAIR-${Date.now()}`,
        status: 'ACTIVE',
      },
    });
    const equipment = await getData<EquipmentEntity>(createRes);
    equipmentId = equipment.id;

    // 创建维修单（reportDate + faultDescription 是必填字段）
    const repairRes = await api.post(`${API_PREFIX}/equipment-repairs`, {
      data: {
        equipmentId: equipment.id,
        reportDate: new Date().toISOString().split('T')[0],
        faultDescription: 'E2E 测试 — 设备故障维修',
      },
    });
    await assertOk(repairRes, 'create repair');
    const repair = await getData<{ id: string; status: string }>(repairRes);
    repairId = repair.id;

    // 查询维修列表
    const listRes = await api.get(`${API_PREFIX}/equipment-repairs?page=1&size=10`);
    await assertOk(listRes, 'list repairs');

    // 完成维修
    const completeRes = await api.post(`${API_PREFIX}/equipment-repairs/${repairId}/complete`, {
      data: { resolution: 'E2E 测试 — 已修复' },
    });
    await assertOk(completeRes, 'complete repair');

    // 清理
    await api.delete(`${API_PREFIX}/equipments/${equipmentId}`);

    console.log('  ✅ 设备维修流程通过');
  });

  // -------------------------------------------------------
  // 设备统计
  // -------------------------------------------------------
  test('统计 — 设备统计接口', async ({ api }) => {
    const res = await api.get(`${API_PREFIX}/dashboard/equipment-stats`);
    await assertOk(res, 'equipment stats');
    const stats = await getData(res);
    expect(stats).toBeDefined();
    console.log('  ✅ 设备统计接口正常');
  });
});
