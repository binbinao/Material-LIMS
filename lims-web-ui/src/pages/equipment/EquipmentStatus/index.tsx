import { PageContainer } from '@ant-design/pro-components';
import { Card, Row, Col, Statistic, Tag, Table } from 'antd';
import { useRequest } from 'ahooks';
import { useIntl } from '@umijs/max';
import { getEquipments, getEquipmentStats } from '@/services/requestService';

const STATUS_COLOR: Record<string, string> = {
  ACTIVE: 'green',
  UNDER_REPAIR: 'red',
  DECOMMISSIONED: 'default',
};

const EquipmentStatus: React.FC = () => {
  const intl = useIntl();
  const { data: statsData } = useRequest(() => getEquipmentStats());
  const { data: equipData } = useRequest(() => getEquipments({ page: 0, size: 100 }));

  const stats = statsData?.data?.statusCounts ?? {};
  const equipments = equipData?.data?.records ?? [];

  const getStatusLabel = (s: string) =>
    intl.formatMessage({ id: `equipment.status.${s}`, defaultMessage: s });

  const columns = [
    { title: intl.formatMessage({ id: 'equipment.name' }), dataIndex: 'name', width: 180 },
    { title: intl.formatMessage({ id: 'equipment.model' }), dataIndex: 'model', width: 150 },
    { title: intl.formatMessage({ id: 'equipment.serialNumber' }), dataIndex: 'serialNumber', width: 150 },
    {
      title: intl.formatMessage({ id: 'equipment.status' }), dataIndex: 'status', width: 130,
      render: (v: string) => <Tag color={STATUS_COLOR[v] || 'default'}>{getStatusLabel(v)}</Tag>,
    },
    { title: intl.formatMessage({ id: 'equipment.location' }), dataIndex: 'location', width: 150 },
    {
      title: intl.formatMessage({ id: 'equipment.warrantyExpiry' }), dataIndex: 'warrantyExpiry', width: 130,
      render: (v: string) => v || '-',
    },
  ];

  return (
    <PageContainer title={intl.formatMessage({ id: 'equipment.status.title' })}>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}>
          <Card><Statistic title={intl.formatMessage({ id: 'equipment.status.active' })} value={stats.ACTIVE || 0} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title={intl.formatMessage({ id: 'equipment.status.underRepair' })} value={stats.UNDER_REPAIR || 0} valueStyle={{ color: '#f5222d' }} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title={intl.formatMessage({ id: 'equipment.status.decommissioned' })} value={stats.DECOMMISSIONED || 0} /></Card>
        </Col>
      </Row>
      <Card title={intl.formatMessage({ id: 'equipment.list.title' })}>
        <Table columns={columns} dataSource={equipments} rowKey="id" size="small" pagination={false} scroll={{ y: 480 }} />
      </Card>
    </PageContainer>
  );
};

export default EquipmentStatus;