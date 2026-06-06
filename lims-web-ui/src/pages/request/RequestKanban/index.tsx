import { PageContainer } from '@ant-design/pro-components';
import { Card, Col, Row, Tag, Typography, Badge, Empty, Spin } from 'antd';
import { useRequest } from 'ahooks';
import { getRequests } from '@/services/requestService';
import dayjs from 'dayjs';

const { Text } = Typography;

const statusColumns = [
  { key: 'SUBMITTED', title: 'Submitted', color: '#1890ff' },
  { key: 'ASSIGNED', title: 'Assigned', color: '#722ed1' },
  { key: 'REPORTING', title: 'Reporting', color: '#fa8c16' },
  { key: 'APPROVING', title: 'Approving', color: '#eb2f96' },
  { key: 'COMPLETED', title: 'Completed', color: '#52c41a' },
];

const RequestKanban: React.FC = () => {
  const { data, loading } = useRequest(() => getRequests({ page: 0, size: 200 }));

  const requests = data?.data?.records ?? [];

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
    <PageContainer title="Request Kanban">
      <Spin spinning={loading}>
        <Row gutter={12}>
          {statusColumns.map((col) => {
            const items = requests.filter((r: any) => r.status === col.key);
            return (
              <Col span={4} key={col.key}>
                <div style={{ marginBottom: 8 }}>
                  <Badge count={items.length} style={{ backgroundColor: col.color }}>
                    <Text strong style={{ fontSize: 14 }}>
                      {col.title}
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
                      style={{ borderLeft: `3px solid ${col.color}` }}
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
