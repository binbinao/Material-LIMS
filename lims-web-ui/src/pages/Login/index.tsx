import { Button, Card, Space, Typography, Divider, App } from 'antd';
import { MicrosoftOutlined } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import { getAuthUrl } from '@/services/requestService';

const { Title, Text, Paragraph } = Typography;

const Login: React.FC = () => {
  const { message } = App.useApp();

  const { loading, run: handleSSO } = useRequest(async () => {
    const result = await getAuthUrl();
    if (result?.data) {
      window.location.href = result.data;
    } else {
      message.error('Failed to get SSO URL');
    }
  }, { manual: true });

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
            icon={<MicrosoftOutlined />}
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
        </Space>
      </Card>
    </div>
  );
};

export default Login;
