import React, { useState } from 'react';
import { Dropdown, Button, Modal, Space, Typography } from 'antd';
import { LogoutOutlined, UserOutlined, ExclamationCircleOutlined } from '@ant-design/icons';

const { Text } = Typography;

/**
 * UserMenu — 右上角用户头像下拉菜单。
 *
 * 这是整个应用的**唯一登出入口**。点击「退出登录」会弹出二次确认对话框，
 * 用户确认后才调用 auth.logout() 进行清理和跳转。
 *
 * 渲染位置：Umi layout 插件的 rightContentRender 回调（见 app.tsx）。
 *
 * 状态说明：
 * - currentUser 存在 → 显示用户名 + 角色 + 退出菜单项
 * - currentUser 不存在 → 显示「未登录」占位，不展示下拉菜单
 */
export default function UserMenu({ initialState }: { initialState?: any }) {
  const user = initialState?.currentUser;
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  const handleConfirmLogout = async () => {
    setLoggingOut(true);
    try {
      const { logout } = await import('../../utils/auth');
      await logout();
      // logout() 内部会 window.location.href = '/login'，
      // 所以这里不需要额外跳转
    } catch {
      // 兜底：即使 logout 抛出异常也强制跳转
      const { forceLogout } = await import('../../utils/auth');
      forceLogout();
    } finally {
      setLoggingOut(false);
      setConfirmOpen(false);
    }
  };

  // 未登录时只显示一个占位按钮，不提供下拉菜单
  if (!user) {
    return (
      <Button type="text" icon={<UserOutlined />} disabled>
        未登录
      </Button>
    );
  }

  const menuItems = [
    {
      key: 'name',
      label: (
        <div style={{ padding: '4px 0' }}>
          <div style={{ fontWeight: 600 }}>{user.displayName || user.username || 'Unknown'}</div>
          <div style={{ fontSize: 12, color: '#999' }}>{user.roles || ''}</div>
        </div>
      ),
      disabled: true,
    },
    { type: 'divider' as const },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: () => setConfirmOpen(true),
    },
  ];

  return (
    <>
      <Dropdown menu={{ items: menuItems }} trigger={['click']}>
        <Button type="text" icon={<UserOutlined />}>
          {user.displayName || user.username || 'Unknown'}
        </Button>
      </Dropdown>

      <Modal
        open={confirmOpen}
        onCancel={() => setConfirmOpen(false)}
        footer={null}
        centered
        closable={false}
        maskClosable={!loggingOut}
        width={400}
      >
        <div style={{ textAlign: 'center', padding: '16px 0' }}>
          <ExclamationCircleOutlined
            style={{ fontSize: 48, color: '#faad14', marginBottom: 16 }}
          />
          <div style={{ marginBottom: 16 }}>
            <Text strong style={{ fontSize: 18, display: 'block', marginBottom: 8 }}>
              确认退出系统？
            </Text>
            {user && (
              <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                当前用户：{user.displayName || user.username}
              </Text>
            )}
            <Text type="secondary" style={{ fontSize: 13 }}>
              退出后需要重新登录才能访问系统，请确保已保存所有工作。
            </Text>
          </div>
          <Space size="middle">
            <Button onClick={() => setConfirmOpen(false)} disabled={loggingOut}>
              取消
            </Button>
            <Button
              type="primary"
              danger
              icon={<LogoutOutlined />}
              onClick={handleConfirmLogout}
              loading={loggingOut}
            >
              确认退出
            </Button>
          </Space>
        </div>
      </Modal>
    </>
  );
}