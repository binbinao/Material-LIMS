import { PageContainer } from '@ant-design/pro-components';
import { Card, Row, Col, Statistic, Tag } from 'antd';
import ReactECharts from 'echarts-for-react';
import { useRequest } from 'ahooks';
import { request } from '@umijs/max';

const statusLabels: Record<string, string> = {
  SUBMITTED: 'Submitted',
  ASSIGNED: 'Assigned',
  SAMPLING: 'Sampling',
  REPORTING: 'Reporting',
  APPROVING: 'Approving',
  COMPLETED: 'Completed',
  REJECTED: 'Rejected',
};

const statusColors: Record<string, string> = {
  SUBMITTED: '#1890ff',
  ASSIGNED: '#722ed1',
  SAMPLING: '#fa8c16',
  REPORTING: '#eb2f96',
  APPROVING: '#faad14',
  COMPLETED: '#52c41a',
  REJECTED: '#f5222d',
};

const RequestDashboard: React.FC = () => {
  const { data } = useRequest(() => request('/api/v1/dashboard/request-stats'));

  const byStatus = data?.data?.byStatus ?? {};
  const byBrand = data?.data?.byBrand ?? {};
  const total = data?.data?.total ?? 0;

  const statusPieOption = {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}: {c}' },
      data: Object.entries(byStatus).map(([key, value]) => ({
        name: statusLabels[key] || key,
        value,
        itemStyle: { color: statusColors[key] },
      })),
    }],
  };

  const brandBarOption = {
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: Object.keys(byBrand) },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: Object.values(byBrand),
      itemStyle: { color: '#1890ff', borderRadius: [4, 4, 0, 0] },
    }],
  };

  return (
    <PageContainer title="Request Dashboard">
      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Card><Statistic title="Total Requests" value={total} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Active" value={Object.entries(byStatus).filter(([k]) => k !== 'COMPLETED' && k !== 'REJECTED').reduce((s, [, v]) => s + (v as number), 0)} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Completed" value={byStatus.COMPLETED || 0} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="Rejected" value={byStatus.REJECTED || 0} valueStyle={{ color: '#f5222d' }} /></Card>
        </Col>
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={12}>
          <Card title="Requests by Status">
            <ReactECharts option={statusPieOption} style={{ height: 300 }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="Requests by Brand">
            <ReactECharts option={brandBarOption} style={{ height: 300 }} />
          </Card>
        </Col>
      </Row>
    </PageContainer>
  );
};

export default RequestDashboard;
