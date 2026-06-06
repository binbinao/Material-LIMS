import { PageContainer } from '@ant-design/pro-components';
import { Card, Row, Col, Statistic, Tag, Table } from 'antd';
import { useRequest } from 'ahooks';
import { getEquipments, getEquipmentStats } from '@/services/requestService';

// Backend canonical statuses (aligned with EquipmentService): ACTIVE / UNDER_REPAIR / DECOMMISSIONED
const STATUS_COLOR: Record<string, string> = {
  ACTIVE: 'green',
  UNDER_REPAIR: 'red',
  DECOMMISSIONED: 'default',
};
const STATUS_LABEL: Record<string, string> = {
  ACTIVE: 'Active',
  UNDER_REPAIR: 'Under Repair',
  DECOMMISSIONED: 'Decommissioned',
};

const EquipmentStatus: React.FC = () => {
  const { data: statsData } = useRequest(() => getEquipmentStats());
  const { data: equipData } = useRequest(() => getEquipments({ page: 0, size: 100 }));

  const stats = statsData?.data?.statusCounts ?? {};
  const equipments = equipData?.data?.records ?? [];

  const columns = [
    { title: 'Name', dataIndex: 'name', width: 180 },
    { title: 'Model', dataIndex: 'model', width: 150 },
    { title: 'Serial No', dataIndex: 'serialNumber', width: 150 },
    {
      title: 'Status', dataIndex: 'status', width: 130,
      render: (v: string) => <Tag color={STATUS_COLOR[v] || 'default'}>{STATUS_LABEL[v] || v}</Tag>,
    },
    { title: 'Location', dataIndex: 'location', width: 150 },
    {
      title: 'Warranty Expiry', dataIndex: 'warrantyExpiry', width: 130,
      render: (v: string) => v || '-',
    },
  ];

  return (
    <PageContainer title="Equipment Status">
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}>
          <Card><Statistic title="Active" value={stats.ACTIVE || 0} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="Under Repair" value={stats.UNDER_REPAIR || 0} valueStyle={{ color: '#f5222d' }} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="Decommissioned" value={stats.DECOMMISSIONED || 0} /></Card>
        </Col>
      </Row>
      <Card title="Equipment List">
        <Table columns={columns} dataSource={equipments} rowKey="id" size="small" pagination={false} scroll={{ y: 480 }} />
      </Card>
    </PageContainer>
  );
};

export default EquipmentStatus;
