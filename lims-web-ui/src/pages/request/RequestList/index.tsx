import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Tag, App } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef } from 'react';
import { history, useIntl } from '@umijs/max';
import { getRequests } from '@/services/requestService';
  const { message } = App.useApp();


const statusColorMap: Record<string, string> = {
  DRAFT: 'default',
  SUBMITTED: 'processing',
  ASSIGNED: 'processing',
  SAMPLING: 'warning',
  REPORTING: 'warning',
  APPROVING: 'warning',
  COMPLETED: 'success',
  REJECTED: 'error',
};

const priorityColorMap: Record<string, string> = {
  LOW: 'default',
  NORMAL: 'blue',
  HIGH: 'orange',
  URGENT: 'red',
};

const RequestList: React.FC = () => {
  const actionRef = useRef<any>();
  const intl = useIntl();

  const columns: ProColumns<any>[] = [
    {
      title: 'Request No',
      dataIndex: 'requestNo',
      width: 150,
      render: (_, record) => <a onClick={() => history.push(`/request/${record.id}`)}>{record.requestNo}</a>,
    },
    { title: 'Brand', dataIndex: 'brandId', width: 100, search: false },
    { title: 'Part Number', dataIndex: 'partNumber', width: 130, ellipsis: true },
    { title: 'Part Name', dataIndex: 'partName', width: 180, ellipsis: true, search: false },
    {
      title: 'Priority',
      dataIndex: 'priority',
      width: 90,
      valueType: 'select',
      valueEnum: {
        LOW: { text: 'Low', status: 'Default' },
        NORMAL: { text: 'Normal', status: 'Processing' },
        HIGH: { text: 'High', status: 'Warning' },
        URGENT: { text: 'Urgent', status: 'Error' },
      },
      render: (_, record) => <Tag color={priorityColorMap[record.priority]}>{record.priority}</Tag>,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      width: 110,
      valueType: 'select',
      valueEnum: {
        DRAFT: { text: 'Draft' },
        SUBMITTED: { text: 'Submitted' },
        ASSIGNED: { text: 'Assigned' },
        SAMPLING: { text: 'Sampling' },
        REPORTING: { text: 'Reporting' },
        APPROVING: { text: 'Approving' },
        COMPLETED: { text: 'Completed', status: 'Success' },
        REJECTED: { text: 'Rejected', status: 'Error' },
      },
      render: (_, record) => <Tag color={statusColorMap[record.status]}>{record.status}</Tag>,
    },
    { title: 'Due Date', dataIndex: 'dueDate', width: 110, valueType: 'date', search: false },
    { title: 'Total Cost', dataIndex: 'totalCost', width: 100, valueType: 'money', search: false },
    { title: 'Created', dataIndex: 'createdAt', width: 150, valueType: 'dateTime', search: false, sorter: true },
  ];

  return (
    <ProTable
      columns={columns}
      actionRef={actionRef}
      request={async (params) => {
        try {
          const result = await getRequests({
            page: params.current,
            size: params.pageSize,
            status: params.status,
            brandId: params.brandId,
            keyword: params.keyword,
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
      toolBarRender={() => [
        <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => history.push('/request/create')}>
          {intl.formatMessage({ id: 'common.create' })}
        </Button>,
      ]}
    />
  );
};

export default RequestList;
