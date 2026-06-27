import { PageContainer } from '@ant-design/pro-components';
import { Card, Table, Tag } from 'antd';
import { useParams, history, useIntl } from '@umijs/max';
import { useRequest } from 'ahooks';
import { getReportRevisions } from '@/services/requestService';
import dayjs from 'dayjs';

const ReportRevisions: React.FC = () => {
  const params = useParams<{ id: string }>();
  const intl = useIntl();

  const { data, loading } = useRequest(() => getReportRevisions(params.id));
  const revisions = data?.data ?? [];

  const getStatusText = (s: string) =>
    intl.formatMessage({ id: `report.status.${s}`, defaultMessage: s });

  const statusColor: Record<string, string> = {
    DRAFT: 'default', IN_REVIEW: 'processing', APPROVED: 'success', REVISING: 'warning',
  };

  const columns = [
    {
      title: intl.formatMessage({ id: 'report.label.version', defaultMessage: 'Version' }), dataIndex: 'versionNumber', width: 100,
      render: (v: string, record: API.Report) => <a onClick={() => history.push(`/report/${record.id}`)}>{v}</a>,
    },
    {
      title: intl.formatMessage({ id: 'report.label.status', defaultMessage: 'Status' }), dataIndex: 'status', width: 120,
      render: (v: string) => <Tag color={statusColor[v]}>{getStatusText(v)}</Tag>,
    },
    { title: intl.formatMessage({ id: 'report.label.revisionNote', defaultMessage: 'Revision Note' }), dataIndex: 'revisionNote', ellipsis: true },
    { title: intl.formatMessage({ id: 'report.label.author', defaultMessage: 'Author' }), dataIndex: 'authorId', width: 120 },
    {
      title: intl.formatMessage({ id: 'report.label.approvedBy', defaultMessage: 'Approved By' }), dataIndex: 'approvedBy', width: 120,
      render: (v: string) => v || '-',
    },
    {
      title: intl.formatMessage({ id: 'report.label.approvedAt', defaultMessage: 'Approved At' }), dataIndex: 'approvedAt', width: 160,
      render: (v: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: intl.formatMessage({ id: 'report.label.created', defaultMessage: 'Created' }), dataIndex: 'createdAt', width: 160,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm'),
    },
  ];

  return (
    <PageContainer
      title={intl.formatMessage({ id: 'report.revisions.title' })}
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
