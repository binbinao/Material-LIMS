import { PageContainer } from '@ant-design/pro-components';
import { Card, Col, Row, Tag, Typography, Badge, Empty, Spin } from 'antd';
import { useRequest } from 'ahooks';
import { history, useIntl } from '@umijs/max';
import { getRequests } from '@/services/requestService';
import dayjs from 'dayjs';

const { Text } = Typography;

const statusKeys = ['DRAFT', 'SUBMITTED', 'ASSIGNED', 'SAMPLING', 'REPORTING', 'APPROVING', 'COMPLETED', 'REJECTED'] as const;

const statusColors: Record<string, string> = {
  DRAFT: '#8c8c8c',
  SUBMITTED: '#1890ff',
  ASSIGNED: '#722ed1',
  SAMPLING: '#13c2c2',
  REPORTING: '#fa8c16',
  APPROVING: '#eb2f96',
  COMPLETED: '#52c41a',
  REJECTED: '#f5222d',
};

const RequestKanban: React.FC = () => {
  const intl = useIntl();
  const { data, loading } = useRequest(() => getRequests({ page: 0, size: 200 }));

  const requests = data?.data?.records ?? [];

  const getStatusLabel = (status: string) =>
    intl.formatMessage({ id: `request.status.${status}`, defaultMessage: status });

  const getDueDateColor = (dueDate: string) => {
    if (!dueDate) return 'default';
    const due = dayjs(dueDate);
    const now = dayjs();
    const diff = due.diff(now, 'day');
    if (diff < 0) return 'red';
    if (diff <= 1) return 'orange';
    if (diff <= 3) return 'gold';
    return 'default';
  };

  return (
    <PageContainer title={intl.formatMessage({ id: 'menu.request.kanban' })}>
      <Spin spinning={loading}>
        <Row gutter={[12, 12]} style={{ overflowX: 'auto', flexWrap: 'nowrap' }}>
          {statusKeys.map((key) => {
            const items = requests.filter((r: any) => r.status === key);
            return (
              <Col flex="220px" key={key}>
                <div style={{ marginBottom: 8 }}>
                  <Badge count={items.length} style={{ backgroundColor: statusColors[key] }}>
                    <Text strong style={{ fontSize: 14 }}>
                      {getStatusLabel(key)}
                    </Text>
                  </Badge>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                  {items.length === 0 && <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />}
                  {items.map((req: any) => (
                    <Card
                      key={req.id}
                      size="small"
                      hoverable
                      role="button"
                      tabIndex={0}
                      aria-label={`${req.requestNo} - ${req.partName || req.partNumber || ''}`}
                      style={{ borderLeft: `3px solid ${statusColors[key]}`, cursor: 'pointer' }}
                      onClick={() => history.push(`/request/${req.id}`)}
                      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); history.push(`/request/${req.id}`); } }}
                    >
                      <div>
                        <Text strong>{req.requestNo}</Text>
                      </div>
                      <div>
                        <Text type="secondary" ellipsis>
                          {req.partName || req.partNumber || '-'}
                        </Text>
                      </div>
                      <div style={{ marginTop: 4 }}>
                        <Tag color={req.priority === 'URGENT' ? 'red' : req.priority === 'HIGH' ? 'orange' : 'blue'} style={{ marginRight: 4 }}>
                          {req.priority}
                        </Tag>
                        {req.dueDate && (
                          <Tag color={getDueDateColor(req.dueDate)}>
                            {dayjs(req.dueDate).format('MMM D')}
                          </Tag>
                        )}
                      </div>
                    </Card>
                  ))}
                </div>
              </Col>
            );
          })}
        </Row>
      </Spin>
    </PageContainer>
  );
};

export default RequestKanban;
