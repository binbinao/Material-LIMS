import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Tag, Button, App } from 'antd';
import { useRef } from 'react';
import { history } from '@umijs/max';
import { getReports } from '@/services/requestService';

const ReportArchive: React.FC = () => {
  const actionRef = useRef<any>();
  const { message } = App.useApp();

  const columns: ProColumns<API.Report>[] = [
    {
      title: 'Report ID', dataIndex: 'id', width: 180,
      render: (_, record) => <a onClick={() => history.push(`/report/${record.id}`)}>{record.id.substring(0, 8)}...</a>,
    },
    { title: 'Request ID', dataIndex: 'requestId', width: 180, search: false },
    { title: 'Version', dataIndex: 'versionNumber', width: 90 },
    { title: 'Author', dataIndex: 'authorId', width: 120, search: false },
    {
      title: 'Status', dataIndex: 'status', width: 110,
      render: (v: string) => <Tag color="default">{v}</Tag>,
    },
    { title: 'Approved', dataIndex: 'approvedAt', width: 150, valueType: 'dateTime', search: false },
    { title: 'Created', dataIndex: 'createdAt', width: 150, valueType: 'dateTime', search: false, sorter: true },
    {
      title: 'Action', width: 100,
      render: (_, record) => <a onClick={() => history.push(`/report/${record.id}`)}>View</a>,
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
            status: 'APPROVED',
          });
          return {
            data: result?.data?.records ?? [],
            total: result?.data?.total ?? 0,
            success: result?.code === 200,
          };
        } catch {
          message.error('Failed to load archived reports');
          return { data: [], total: 0, success: false };
        }
      }}
      rowKey="id"
      search={{ labelWidth: 'auto' }}
    />
  );
};

export default ReportArchive;
