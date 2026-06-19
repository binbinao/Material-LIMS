import { PageContainer } from '@ant-design/pro-components';
import { Card, Row, Col, Statistic, Typography, Table, Tag, Spin, List, Button } from 'antd';
import { useModel, history } from '@umijs/max';
import { useRequest } from 'ahooks';
import { getMyDashboard, getMyPendingTasks } from '@/services/requestService';

const Workspace: React.FC = () => {
  const { initialState } = useModel('@@initialState');
  const currentUser = initialState?.currentUser;

  const { data: dashboardData, loading } = useRequest(
    () => getMyDashboard(currentUser?.id || ''),
    { refreshDeps: [currentUser?.id] },
  );
  const { data: tasksData, loading: tasksLoading } = useRequest(() => getMyPendingTasks());

  const stats = dashboardData?.data;
  const requestStats = stats?.requestStats ?? {};
  const pendingTasks = tasksData?.data ?? [];

  return (
    <PageContainer>
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card>
            <Typography.Title level={4}>
              Welcome, {currentUser?.displayName || 'User'}
            </Typography.Title>
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable onClick={() => history.push('/request/list')}>
            <Statistic title="Draft Requests" value={requestStats.DRAFT || 0} />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable onClick={() => history.push('/request/kanban')}>
            <Statistic title="Active Requests" value={(requestStats.SUBMITTED || 0) + (requestStats.ASSIGNED || 0)} />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable>
            <Statistic title="Pending Tasks" value={stats?.pendingTasks || 0} />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable>
            <Statistic title={<span role="alert" aria-label="Overdue tasks">Overdue</span>} value={stats?.overdue || 0} valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="My Pending Workflow Tasks" extra={<Button size="small" onClick={() => history.push('/request/kanban')}>View All</Button>}>
            <Spin spinning={tasksLoading}>
              <List
                dataSource={pendingTasks.slice(0, 5)}
                locale={{ emptyText: 'No pending tasks' }}
                renderItem={(item: any) => (
                  <List.Item
                    actions={[<a key="go" onClick={() => history.push(`/request/${item.requestId}`)}>View</a>]}
                  >
                    <List.Item.Meta
                      title={<span>{item.taskName}</span>}
                      description={<span style={{ fontSize: 12 }}>Request: {item.requestId?.substring(0, 8)}...</span>}
                    />
                  </List.Item>
                )}
              />
            </Spin>
          </Card>
        </Col>
        <Col span={12}>
          <Card title="Request Status Summary">
            <Row gutter={8}>
              {Object.entries(requestStats).map(([status, count]) => (
                <Col span={8} key={status} style={{ marginBottom: 8 }}>
                  <Statistic title={status} value={count as number} />
                </Col>
              ))}
            </Row>
          </Card>
        </Col>
      </Row>
    </PageContainer>
  );
};

export default Workspace;
