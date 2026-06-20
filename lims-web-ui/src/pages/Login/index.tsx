import { Button, Card, Form, Input, Space, Typography, Divider, App } from 'antd';
import { UserOutlined, LockOutlined, LoginOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { useState } from 'react';
import { history, useModel } from '@umijs/max';
import { login } from '@/services/requestService';

const { Title, Text, Paragraph } = Typography;

const Login: React.FC = () => {
  const { message } = App.useApp();
  const { setInitialState } = useModel('@@initialState');
  const [loginForm] = Form.useForm();
  const [devForm] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const isDev = process.env.NODE_ENV !== 'production';

  /**
   * Manual password login. Default password for every seeded account
   * is "password" (V15). The server sets the LIMS_TOKEN auth cookie
   * and returns {token, user, expiresInHours}; we mirror the user
   * into initialState so the rest of the UI can read it without a
   * second /auth/me round trip.
   */
  const handleLogin = async (values: { loginId: string; password: string }) => {
    setSubmitting(true);
    try {
      const res = await login(values.loginId, values.password);
      if (res?.code !== 200 || !res?.data?.user) {
        message.error('Login failed: ' + (res?.message || 'no user'));
        return;
      }
      // Clear any stale dev_user from a prior quick-login session so
      // the requestInterceptor (app.tsx) doesn't keep sending it.
      window.localStorage.removeItem('dev_user');
      await setInitialState((s: any) => ({ ...s, currentUser: res.data.user }));
      message.success('Welcome, ' + res.data.user.displayName);
      const params = new URLSearchParams(window.location.search);
      const redirect = params.get('redirect') || '/';
      history.push(redirect);
    } catch (e: any) {
      message.error(e?.message || 'Login error');
    } finally {
      setSubmitting(false);
    }
  };

  /**
   * Dev quick login: the DevAuthFilter on the backend synthesizes a
   * principal from the X-Dev-User header without a real JWT, so we
   * just hit /auth/me with the chosen username. Kept for tests, demos,
   * and smoke probes — production builds hide it via the isDev guard
   * below.
   */
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
          <Form
            form={loginForm}
            layout="vertical"
            onFinish={handleLogin}
            initialValues={{ loginId: 'admin', password: 'password' }}
            style={{ width: '100%' }}
          >
            <Form.Item
              name="loginId"
              label="Username"
              rules={[{ required: true, message: 'Enter a username' }]}
            >
              <Input prefix={<UserOutlined />} placeholder="admin" autoComplete="username" />
            </Form.Item>
            <Form.Item
              name="password"
              label="Password"
              rules={[{ required: true, message: 'Enter a password' }]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="password"
                autoComplete="current-password"
              />
            </Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              icon={<LoginOutlined />}
              loading={submitting}
            >
              Sign in
            </Button>
          </Form>

          <Paragraph type="secondary" style={{ textAlign: 'center', fontSize: 12, marginBottom: 0 }}>
            Default password is <code>password</code> for every seeded account. Change it
            from your account settings after first sign-in.
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
                  <Input prefix={<ThunderboltOutlined />} placeholder="admin" autoComplete="username" />
                </Form.Item>
                <Button
                  type="default"
                  htmlType="submit"
                  block
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
