import { PageContainer } from '@ant-design/pro-components';
import { Card, Row, Col, Statistic } from 'antd';
import ReactECharts from 'echarts-for-react';
import { useRequest } from 'ahooks';
import { request, useIntl } from '@umijs/max';

/** 状态颜色映射（不需要翻译） */
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
  const intl = useIntl();
  const { data } = useRequest(() => request('/api/v1/dashboard/request-stats'));

  const byStatus = data?.data?.byStatus ?? {};
  const byBrand = data?.data?.byBrand ?? {};
  const total = data?.data?.total ?? 0;

  const getStatusLabel = (key: string) =>
    intl.formatMessage({ id: `request.status.${key}`, defaultMessage: key });

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
        name: getStatusLabel(key),
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
    <PageContainer title={intl.formatMessage({ id: 'dashboard.requestDashboard' })}>
      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Card><Statistic title={intl.formatMessage({ id: 'dashboard.totalRequests' })} value={total} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title={intl.formatMessage({ id: 'dashboard.active' })} value={Object.entries(byStatus).filter(([k]) => k !== 'COMPLETED' && k !== 'REJECTED').reduce((s, [, v]) => s + (v as number), 0)} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title={intl.formatMessage({ id: 'dashboard.completed' })} value={byStatus.COMPLETED || 0} valueStyle={{ color: '#52c41a' }} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title={intl.formatMessage({ id: 'dashboard.rejected' })} value={byStatus.REJECTED || 0} valueStyle={{ color: '#f5222d' }} /></Card>
        </Col>
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={12}>
          <Card title={intl.formatMessage({ id: 'dashboard.requestsByStatus' })}>
            <ReactECharts option={statusPieOption} style={{ height: 300 }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card title={intl.formatMessage({ id: 'dashboard.requestsByBrand' })}>
            <ReactECharts option={brandBarOption} style={{ height: 300 }} />
          </Card>
        </Col>
      </Row>
    </PageContainer>
  );
};

export default RequestDashboard;