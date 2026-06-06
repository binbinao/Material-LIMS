import { PageContainer } from '@ant-design/pro-components';
import { Card, Row, Col, Statistic, Table } from 'antd';
import ReactECharts from 'echarts-for-react';
import { useRequest } from 'ahooks';
import { useMemo } from 'react';
import { getCostStats } from '@/services/requestService';

const toNumber = (value: unknown): number => {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
};

const CostDashboard: React.FC = () => {
  const { data } = useRequest(() => getCostStats());

  const payload = data?.data;
  const totalCost = toNumber(payload?.totalCost);
  const requestCount = toNumber(payload?.requestCount);
  const costByBrand = (payload?.costByBrand ?? payload?.data ?? {}) as Record<string, unknown>;

  const breakdown = useMemo(
    () =>
      Object.entries(costByBrand).map(([brand, cost]) => ({
        brand,
        cost: toNumber(cost),
      })),
    [costByBrand],
  );

  const barOption = useMemo(
    () => ({
      tooltip: { trigger: 'axis', formatter: '{b}: ¥{c}' },
      xAxis: { type: 'category', data: breakdown.map((item) => item.brand) },
      yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
      series: [{
        type: 'bar',
        data: breakdown.map((item) => item.cost),
        itemStyle: { color: '#722ed1', borderRadius: [4, 4, 0, 0] },
      }],
    }),
    [breakdown],
  );

  const tableData = breakdown.map((item, idx) => ({
    key: idx,
    brand: item.brand,
    cost: item.cost,
    percentage: totalCost > 0 ? ((item.cost / totalCost) * 100).toFixed(1) : '0',
  }));

  const columns = [
    { title: 'Brand', dataIndex: 'brand' },
    { title: 'Cost (¥)', dataIndex: 'cost', render: (v: number) => v?.toFixed(2) },
    { title: 'Percentage', dataIndex: 'percentage', render: (v: string) => `${v}%` },
  ];

  return (
    <PageContainer title="Cost Dashboard">
      <Row gutter={[16, 16]}>
        <Col span={8}>
          <Card><Statistic title="Total Cost" value={totalCost} precision={2} prefix="¥" valueStyle={{ color: '#722ed1' }} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="Total Requests" value={requestCount} /></Card>
        </Col>
        <Col span={8}>
          <Card><Statistic title="Avg Cost per Request" value={requestCount > 0 ? totalCost / requestCount : 0} precision={2} prefix="¥" /></Card>
        </Col>
      </Row>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col span={14}>
          <Card title="Cost by Brand">
            <ReactECharts option={barOption} notMerge style={{ height: 350 }} />
          </Card>
        </Col>
        <Col span={10}>
          <Card title="Cost Breakdown">
            <Table dataSource={tableData} columns={columns} pagination={false} size="small" />
          </Card>
        </Col>
      </Row>
    </PageContainer>
  );
};

export default CostDashboard;
