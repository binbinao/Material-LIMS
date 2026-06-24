import { test, expect } from './fixtures/api';
import {
  API_PREFIX,
  assertOk,
  getData,
} from './fixtures/api';

/**
 * 🟡 中优先级 — 系统管理 E2E 测试
 *
 * 覆盖：用户管理（列表/角色/启禁用）、审计日志查询
 */
test.describe('Admin — 系统管理', () => {
  // -------------------------------------------------------
  // 用户列表
  // -------------------------------------------------------
  test('用户管理 — 列表 + 分页 + 搜索', async ({ api }) => {
    // 基础列表
    const res = await api.get(`${API_PREFIX}/admin/users?page=1&size=10`);
    await assertOk(res, 'list users');
    const page = await getData<{
      records: { id: string; displayName: string; email: string }[];
      total: number;
    }>(res);

    expect(page.records.length).toBeGreaterThan(0);
    expect(page.total).toBeGreaterThanOrEqual(page.records.length);

    // 验证 dev 用户存在
    const devUser = page.records.find((u) => u.id === 'dev-user-0001');
    expect(devUser).toBeDefined();

    // 搜索
    const searchRes = await api.get(
      `${API_PREFIX}/admin/users?page=1&size=10&keyword=dev`
    );
    await assertOk(searchRes, 'search users');
    const searchPage = await getData<{
      records: { displayName: string }[];
    }>(searchRes);
    expect(searchPage.records.length).toBeGreaterThan(0);

    console.log(`  ✅ 用户列表: total=${page.total}`);
  });

  // -------------------------------------------------------
  // 用户角色管理
  // -------------------------------------------------------
  test('用户管理 — 更新角色', async ({ api }) => {
    const res = await api.put(`${API_PREFIX}/admin/users/dev-user-0001/roles`, {
      data: { roles: 'ADMIN,ENGINEER' },
    });
    await assertOk(res, 'update roles');

    // 验证
    const listRes = await api.get(`${API_PREFIX}/admin/users?keyword=dev`);
    const page = await getData<{
      records: { id: string; roles: string }[];
    }>(listRes);
    const devUser = page.records.find((u) => u.id === 'dev-user-0001');
    expect(devUser?.roles).toContain('ADMIN');

    // 恢复
    await api.put(`${API_PREFIX}/admin/users/dev-user-0001/roles`, {
      data: { roles: 'ADMIN,MANAGER,ENGINEER,REQUESTER,TECHNICIAN' },
    });

    console.log('  ✅ 角色更新成功');
  });

  // -------------------------------------------------------
  // 用户启/禁用
  // -------------------------------------------------------
  test('用户管理 — 切换激活状态', async ({ api }) => {
    // 禁用
    const res1 = await api.put(
      `${API_PREFIX}/admin/users/dev-user-0001/toggle-active`
    );
    await assertOk(res1, 'toggle active off');

    // 恢复
    const res2 = await api.put(
      `${API_PREFIX}/admin/users/dev-user-0001/toggle-active`
    );
    await assertOk(res2, 'toggle active on');

    console.log('  ✅ 激活状态切换正常');
  });

  // -------------------------------------------------------
  // 审计日志
  // -------------------------------------------------------
  test('审计日志 — 列表 + 过滤', async ({ api }) => {
    // 基础列表
    const res = await api.get(`${API_PREFIX}/admin/logs?page=1&size=10`);
    await assertOk(res, 'list logs');
    const result = await getData<{
      records: { module: string; action: string; userName: string }[];
      total: number;
    }>(res);

    expect(Array.isArray(result.records)).toBe(true);
    expect(result.total).toBeGreaterThanOrEqual(0);

    // 按模块过滤
    const filterRes = await api.get(
      `${API_PREFIX}/admin/logs?page=1&size=10&module=REQUEST`
    );
    await assertOk(filterRes, 'filter logs by module');
    const filtered = await getData<{
      records: { module: string }[];
    }>(filterRes);

    if (filtered.records.length > 0) {
      filtered.records.forEach((r) => {
        expect(r.module).toBe('REQUEST');
      });
    }

    // 日志详情
    if (result.records.length > 0) {
      const firstId = (result.records[0] as { id: string }).id;
      if (firstId) {
        const detailRes = await api.get(`${API_PREFIX}/admin/logs/${firstId}`);
        await assertOk(detailRes, 'get log detail');
      }
    }

    console.log(`  ✅ 审计日志: total=${result.total}`);
  });
});
