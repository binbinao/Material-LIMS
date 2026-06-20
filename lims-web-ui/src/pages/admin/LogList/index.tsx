import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Tag, Modal, Descriptions, Typography, App } from 'antd';
import { useRef, useState } from 'react';
import { getAdminLogs } from '@/services/requestService';


const MODULE_COLOR: Record<string, string> = {
  REQUEST: 'blue', REPORT: 'cyan', EQUIPMENT: 'purple', EQUIPMENT_REPAIR: 'magenta',
  BRAND: 'orange', I18N: 'green', KNOWLEDGE: 'gold', HOLIDAY: 'lime',
  REQUEST_TYPE: 'volcano', DEPARTMENT: 'geekblue', ANALYSIS_ITEM: 'cyan',
};

const LogList: React.FC = () => {
  const actionRef = useRef<any>();
  const [detailOpen, setDetailOpen] = useState(false);
  const [activeLog, setActiveLog] = useState<any>(null);
  const { message } = App.useApp();

  const columns: ProColumns[] = [
    { title: 'Timestamp', dataIndex: 'createdAt', width: 170, valueType: 'dateTime', sorter: true, search: false },
    {
      title: 'Date Range', dataIndex: 'dateRange', valueType: 'dateRange', hideInTable: true,
      search: { transform: (v: [string, string]) => ({ startDate: v?.[0], endDate: v?.[1] }) },
    },
    { title: 'User', dataIndex: 'userName', width: 140 },
    {
      title: 'Module', dataIndex: 'module', width: 130,
      valueType: 'select',
      valueEnum: {
        REQUEST: { text: 'Request' },
        REPORT: { text: 'Report' },
        EQUIPMENT: { text: 'Equipment' },
        EQUIPMENT_REPAIR: { text: 'Equipment Repair' },
        KNOWLEDGE: { text: 'Knowledge' },
        BRAND: { text: 'Brand' },
        REQUEST_TYPE: { text: 'Request Type' },
        DEPARTMENT: { text: 'Department' },
        HOLIDAY: { text: 'Holiday' },
        ANALYSIS_ITEM: { text: 'Analysis Item' },
        I18N: { text: 'I18n' },
      },
      render: (_, r: any) => <Tag color={MODULE_COLOR[r.module] || 'default'}>{r.module}</Tag>,
    },
    { title: 'Action', dataIndex: 'action', width: 120 },
    { title: 'Entity', dataIndex: 'entityId', width: 160, search: false, ellipsis: true },
    { title: 'IP', dataIndex: 'ip', width: 130, search: false },
    {
      title: 'Detail', valueType: 'option', width: 80, fixed: 'right',
      render: (_, r: any) => [<a key="d" onClick={() => { setActiveLog(r); setDetailOpen(true); }}>View</a>],
    },
  ];

  return (
    <>
      <ProTable
        columns={columns}
        actionRef={actionRef}
        rowKey="id"
        scroll={{ x: 1100 }}
        request={async (params) => {
          try {
            const r = await getAdminLogs({
              page: (params.current || 1) - 1,
              size: params.pageSize,
              module: params.module,
              action: params.action,
              userId: params.userId,
              startDate: params.startDate,
              endDate: params.endDate,
            });
            return { data: r?.data?.records ?? [], total: r?.data?.total ?? 0, success: r?.code === 200 };
          } catch (e: any) {
          message.error(e?.message || 'Load failed');
          return { data: [], total: 0, success: false };
        }
        }}
        search={{ labelWidth: 'auto' }}
      />

      <Modal title="Audit Log Detail" open={detailOpen} footer={null} width={700}
        onCancel={() => setDetailOpen(false)}>
        {activeLog && (
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="Time">{activeLog.createdAt}</Descriptions.Item>
            <Descriptions.Item label="User">{activeLog.userName} ({activeLog.userId})</Descriptions.Item>
            <Descriptions.Item label="Module">{activeLog.module}</Descriptions.Item>
            <Descriptions.Item label="Action">{activeLog.action}</Descriptions.Item>
            <Descriptions.Item label="Entity ID">{activeLog.entityId || '-'}</Descriptions.Item>
            <Descriptions.Item label="IP">{activeLog.ip}</Descriptions.Item>
            <Descriptions.Item label="Payload">
              <Typography.Paragraph
                copyable
                style={{ marginBottom: 0, whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 12 }}>
                {(() => {
                  try { return JSON.stringify(JSON.parse(activeLog.detail || '{}'), null, 2); }
                  catch { return activeLog.detail || ''; }
                })()}
              </Typography.Paragraph>
            </Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </>
  );
};

export default LogList;
