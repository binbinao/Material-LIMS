import React from 'react';
import { Dropdown, Button, Modal } from 'antd';
import { LogoutOutlined, UserOutlined } from '@ant-design/icons';

/**
 * UserMenu — top-right Dropdown shown by the Umi layout's
 * `actionsRender` callback. Displays the current user's displayName
 * and a Logout item. Logout now goes through Modal.confirm so a
 * misclick doesn't kick the user out of the app.
 */
export default function UserMenu({ initialState }: { initialState?: any }) {
  const user = initialState?.currentUser;

  const confirmLogout = () => {
    Modal.confirm({
      title: '确认退出登录？',
      okText: '退出',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: async () => {
        const { logout } = await import('../../utils/auth');
        await logout();
      },
    });
  };

  return (
    <Dropdown
      menu={{
        items: [
          {
            key: 'name',
            label: (
              <div style={{ padding: '4px 0' }}>
                <div style={{ fontWeight: 600 }}>{user?.displayName || 'Unknown'}</div>
                <div style={{ fontSize: 12, color: '#999' }}>{user?.roles || ''}</div>
              </div>
            ),
            disabled: true,
          },
          { type: 'divider' as const },
          {
            key: 'logout',
            icon: <LogoutOutlined />,
            label: 'Logout',
            onClick: confirmLogout,
          },
        ],
      }}
    >
      <Button type="text" icon={<UserOutlined />}>
        {user?.displayName || 'Guest'}
      </Button>
    </Dropdown>
  );
}
