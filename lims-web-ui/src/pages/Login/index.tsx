import { Button, Card, Form, Input, Space, Typography, Divider, App } from 'antd';
import { WindowsOutlined, LoginOutlined } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import { useState } from 'react';
import { history, useModel } from '@umijs/max';
import { getAuthUrl } from '@/services/requestService';

const { Title, Text, Paragraph } = Typography;

const Login: React.FC = () => {
  const { message } = App.useApp();
  const { setInitialState } = useModel('@@initialState');
  const [devForm] = Form.useForm();
  const isDev = process.env.NODE_ENV !== 'production';

  const { loading, run: handleSSO } = useRequest(async () => {
    const result = await getAuthUrl();
    if (result?.data) {
      window.location.href = result.data;
    } else {
      message.error('Failed to get SSO URL');
    }
  }, { manual: true });

  // Dev-only quick login: the DevAuthFilter on the backend synthesizes an
  // ADMIN-equivalent principal from the X-Dev-User header. The username is
  // stored in localStorage so app.tsx's requestInterceptor can re-attach
  // the header on every subsequent API call (including page reloads).
  // Hidden in prod via the isDev guard below.
  const handleDevLogin = async (values: { username: string }) => {
    try {
      const me = await fetch('/api/v1/auth/me', {
        headers: { 'X-Dev-User': values.username, 'Accept': 'application/json' },
        credentials: 'include',
      }).then((r) => r.json());
      if (me?.code !== 200 || !me?.data?.id) {
        message.error('Dev login failed: ' + (me?.message || 'no user'));
        return;
      }
      // Persist for the requestInterceptor (app.tsx) to pick up.
      window.localStorage.setItem('dev_user', values.username);
      await setInitialState((s: any) => ({ ...s, currentUser: me.data }));
      message.success('Logged in as ' + me.data.displayName);
      const params = new URLSearchParams(window.location.search);
      const redirect = params.get('redirect') || '/';
      history.push(redirect);
    } catch (e: any) {
      message.error(e?.message || 'Dev login error');
    }
  };

  return (
    <div style={{
      display: 'flex', justifyContent: 'center', alignItems: 'center',
      minHeight: '100vh', background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    }}>
      <Card style={{ width: 420, borderRadius: 12, boxShadow: '0 8px 24px rgba(0,0,0,0.15)' }}>
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <Title level={2} style={{ marginBottom: 8 }}>Material LIMS</Title>
          <Text type="secondary">Laboratory Information Management System</Text>
        </div>

        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Button
            type="primary"
            size="large"
            block
            icon={<WindowsOutlined />}
            loading={loading}
            onClick={handleSSO}
            style={{ height: 48, fontSize: 16 }}
          >
            Sign in with Microsoft 365
          </Button>

          <Divider style={{ margin: '8px 0' }}>
            <Text type="secondary" style={{ fontSize: 12 }}>Secure SSO</Text>
          </Divider>

          <Paragraph type="secondary" style={{ textAlign: 'center', fontSize: 12, marginBottom: 0 }}>
            Access is managed through Azure AD. Contact your administrator if you cannot sign in.
          </Paragraph>

          {isDev && (
            <>
              <Divider style={{ margin: '8px 0' }}>
                <Text type="secondary" style={{ fontSize: 12 }}>Dev Quick Login</Text>
              </Divider>
              <Form
                form={devForm}
                layout="vertical"
                onFinish={handleDevLogin}
                initialValues={{ username: 'admin' }}
                style={{ width: '100%' }}
              >
                <Form.Item
                  name="username"
                  label={<Text type="secondary" style={{ fontSize: 12 }}>Username</Text>}
                  rules={[{ required: true, message: 'Enter a username' }]}
                >
                  <Input placeholder="admin" autoComplete="username" />
                </Form.Item>
                <Button
                  type="default"
                  htmlType="submit"
                  block
                  icon={<LoginOutlined />}
                >
                  Dev Login (no password)
                </Button>
                <Paragraph type="secondary" style={{ fontSize: 11, marginTop: 8, marginBottom: 0 }}>
                  DevAuthFilter auto-injects roles for the user. Try
                  <code> admin</code>, <code>manager</code>, <code>engineer</code>, or <code>requester</code>.
                </Paragraph>
              </Form>
            </>
          )}
        </Space>
      </Card>
    </div>
  );
};

export default Login;
