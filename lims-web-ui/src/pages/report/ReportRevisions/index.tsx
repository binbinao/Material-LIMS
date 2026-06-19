import { PageContainer } from '@ant-design/pro-components';
import { Card, Table, Tag, App } from 'antd';
import { useParams, history } from '@umijs/max';
import { useRequest } from 'ahooks';
import { getReportRevisions } from '@/services/requestService';
import dayjs from 'dayjs';

const reportStatusMap: Record<string, { color: string; text: string }> = {
  DRAFT: { color: 'default', text: 'Draft' },
  IN_REVIEW: { color: 'processing', text: 'In Review' },
  APPROVED: { color: 'success', text: 'Approved' },
  REVISING: { color: 'warning', text: 'Revising' },
};

const ReportRevisions: React.FC = () => {
  const params = useParams<{ id: string }>();

  const { data, loading } = useRequest(() => getReportRevisions(params.id));
  const revisions = data?.data ?? [];

  const columns = [
    {
      title: 'Version', dataIndex: 'versionNumber', width: 100,
      render: (v: string, record: API.Report) => <a onClick={() => history.push(`/report/${record.id}`)}>{v}</a>,
    },
    {
      title: 'Status', dataIndex: 'status', width: 120,
      render: (v: string) => <Tag color={reportStatusMap[v]?.color}>{reportStatusMap[v]?.text || v}</Tag>,
    },
    { title: 'Revision Note', dataIndex: 'revisionNote', ellipsis: true },
    { title: 'Author', dataIndex: 'authorId', width: 120 },
    {
      title: 'Approved By', dataIndex: 'approvedBy', width: 120,
      render: (v: string) => v || '-',
    },
    {
      title: 'Approved At', dataIndex: 'approvedAt', width: 160,
      render: (v: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: 'Created', dataIndex: 'createdAt', width: 160,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm'),
    },
  ];

  return (
    <PageContainer
      title="Report Revisions"
      onBack={() => history.push(`/report/${params.id}`)}
    >
      <Card>
        <Table
          columns={columns}
          dataSource={revisions}
          rowKey="id"
          loading={loading}
          pagination={false}
          size="small"
        />
      </Card>
    </PageContainer>
  );
};

export default ReportRevisions;
