import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Tag, Button, App } from 'antd';
import { useRef } from 'react';
import { history, useIntl } from '@umijs/max';
import { getReports } from '@/services/requestService';

const reportStatusMap: Record<string, { color: string; text: string }> = {
  DRAFT: { color: 'default', text: 'Draft' },
  IN_REVIEW: { color: 'processing', text: 'In Review' },
  APPROVED: { color: 'success', text: 'Approved' },
  REVISING: { color: 'warning', text: 'Revising' },
};

const ReportList: React.FC = () => {
  const actionRef = useRef<any>();
  const intl = useIntl();
  const { message } = App.useApp();

  const columns: ProColumns<API.Report>[] = [
    {
      title: 'Report ID',
      dataIndex: 'id',
      width: 180,
      render: (_, record) => <a onClick={() => history.push(`/report/${record.id}`)}>{record.id.substring(0, 8)}...</a>,
    },
    { title: 'Request ID', dataIndex: 'requestId', width: 180, search: false, render: (v: string) => v?.substring(0, 8) + '...' },
    { title: 'Version', dataIndex: 'versionNumber', width: 90 },
    { title: 'Author', dataIndex: 'authorId', width: 120, search: false },
    {
      title: 'Status',
      dataIndex: 'status',
      width: 110,
      valueType: 'select',
      valueEnum: Object.fromEntries(
        Object.entries(reportStatusMap).map(([k, v]) => [k, { text: v.text }])
      ),
      render: (_, record) => (
        <Tag color={reportStatusMap[record.status]?.color}>{reportStatusMap[record.status]?.text || record.status}</Tag>
      ),
    },
    { title: 'Submitted', dataIndex: 'submittedAt', width: 150, valueType: 'dateTime', search: false },
    { title: 'Approved', dataIndex: 'approvedAt', width: 150, valueType: 'dateTime', search: false },
    { title: 'Created', dataIndex: 'createdAt', width: 150, valueType: 'dateTime', search: false, sorter: true },
    {
      title: 'Download',
      dataIndex: 'id',
      width: 120,
      search: false,
      render: (_, record) => (
        <a href={`/api/v1/reports/${record.id}/sample-word`} target="_blank" rel="noopener">
          Word
        </a>
      ),
    },
  ];

  return (
    <ProTable<API.Report>
      columns={columns}
      actionRef={actionRef}
      request={async (params) => {
        try {
          const result = await getReports({
            page: params.current,
            size: params.pageSize,
            status: params.status,
            requestId: params.requestId,
          });
          return {
            data: result?.data?.records ?? [],
            total: result?.data?.total ?? 0,
            success: result?.code === 200,
          };
        } catch (e: any) {
          message.error(e?.message || 'Load failed');
          return { data: [], total: 0, success: false };
        }
      }}
      rowKey="id"
      search={{ labelWidth: 'auto' }}
    />
  );
};

export default ReportList;
