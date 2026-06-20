import React from 'react';
import { Dropdown, Button } from 'antd';
import { LogoutOutlined, UserOutlined } from '@ant-design/icons';

/**
 * UserMenu — top-right Dropdown shown by the Umi layout's
 * `actionsRender` callback. Displays the current user's displayName
 * and a Logout item that clears the dev-login marker and redirects
 * to /login. Extracted to a .tsx file because Umi's config parser
 * (esbuild without JSX transform) does not support inline JSX in
 * .umirc.ts.
 */
export default function UserMenu({ initialState }: { initialState?: any }) {
  const user = initialState?.currentUser;
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
            onClick: async () => {
              const { logout } = await import('../../utils/auth');
              logout();
            },
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
