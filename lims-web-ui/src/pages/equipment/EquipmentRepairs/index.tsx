import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, Select, InputNumber, App, Tag, Space, Popconfirm } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import {
  getEquipmentRepairs,
  createEquipmentRepair,
  completeEquipmentRepair,
  deleteEquipmentRepair,
  getEquipments,
} from '@/services/requestService';

const STATUS_COLOR: Record<string, string> = {
  REPORTING: 'orange',
  REPAIRING: 'blue',
  COMPLETED: 'green',
};

const EquipmentRepairs: React.FC = () => {
  const actionRef = useRef<any>();
  const [createOpen, setCreateOpen] = useState(false);
  const [completeOpen, setCompleteOpen] = useState(false);
  const [activeRecord, setActiveRecord] = useState<any>(null);
  const [createForm] = Form.useForm();
  const [completeForm] = Form.useForm();
  const { message } = App.useApp();
  const [equipmentOptions, setEquipmentOptions] = useState<any[]>([]);

  const loadEquipments = async () => {
    if (equipmentOptions.length) return;
    const r = await getEquipments({ page: 0, size: 200 });
    setEquipmentOptions(
      (r?.data?.records ?? []).map((e: any) => ({ label: `${e.name} (${e.serialNumber || e.model || ''})`, value: e.id })),
    );
  };

  const handleCreate = async () => {
    const values = await createForm.validateFields();
    try {
      await createEquipmentRepair(values);
      message.success('Repair record created; equipment marked as UNDER_REPAIR');
      setCreateOpen(false);
      createForm.resetFields();
      actionRef.current?.reload();
    } catch (e: any) {
      message.error(e?.message || 'Failed');
    }
  };

  const handleComplete = async () => {
    const values = await completeForm.validateFields();
    try {
      await completeEquipmentRepair(activeRecord.id, values);
      message.success('Repair completed');
      setCompleteOpen(false);
      completeForm.resetFields();
      actionRef.current?.reload();
    } catch (e: any) {
      message.error(e?.message || 'Failed');
    }
  };

  const columns: ProColumns[] = [
    { title: 'Report Date', dataIndex: 'reportDate', width: 120, valueType: 'date' },
    { title: 'Equipment', dataIndex: 'equipmentId', width: 200, search: false },
    { title: 'Fault', dataIndex: 'faultDescription', ellipsis: true, search: false },
    { title: 'Reported By', dataIndex: 'reportedBy', width: 120, search: false },
    {
      title: 'Status', dataIndex: 'status', width: 110,
      valueType: 'select',
      valueEnum: {
        REPORTING: { text: 'Reporting' },
        REPAIRING: { text: 'Repairing' },
        COMPLETED: { text: 'Completed' },
      },
      render: (_, r: any) => <Tag color={STATUS_COLOR[r.status] || 'default'}>{r.status}</Tag>,
    },
    { title: 'Repair Cost', dataIndex: 'repairCost', width: 110, valueType: 'money', search: false },
    { title: 'Completion', dataIndex: 'completionDate', width: 120, valueType: 'date', search: false },
    {
      title: 'Action', valueType: 'option', width: 160, fixed: 'right',
      render: (_, r: any) => [
        r.status !== 'COMPLETED' && (
          <a key="complete" onClick={() => { setActiveRecord(r); setCompleteOpen(true); }}>Complete</a>
        ),
        <Popconfirm key="del" title="Delete this repair record?" onConfirm={async () => {
          await deleteEquipmentRepair(r.id);
          message.success('Deleted');
          actionRef.current?.reload();
        }}><a style={{ color: '#f5222d' }}>Delete</a></Popconfirm>,
      ].filter(Boolean),
    },
  ];

  return (
    <>
      <ProTable
        columns={columns}
        actionRef={actionRef}
        rowKey="id"
        scroll={{ x: 1200 }}
        request={async (params) => {
          try {
            const r = await getEquipmentRepairs({ page: (params.current || 1) - 1, size: params.pageSize, status: params.status });
            return { data: r?.data?.records ?? [], total: r?.data?.total ?? 0, success: r?.code === 200 };
          } catch (e: any) {
          message.error(e?.message || 'Load failed');
          return { data: [], total: 0, success: false };
        }
        }}
        search={{ labelWidth: 'auto' }}
        toolBarRender={() => [
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => { loadEquipments(); setCreateOpen(true); }}>
            Report Repair
          </Button>,
        ]}
      />

      <Modal title="Report Equipment Repair" open={createOpen}
        onOk={handleCreate} onCancel={() => { setCreateOpen(false); createForm.resetFields(); }}
        destroyOnClose>
        <Form form={createForm} layout="vertical">
          <Form.Item name="equipmentId" label="Equipment" rules={[{ required: true }]}>
            <Select showSearch options={equipmentOptions} placeholder="Select equipment" optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="faultDescription" label="Fault Description" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="reportedBy" label="Reported By"><Input /></Form.Item>
        </Form>
      </Modal>

      <Modal title="Complete Repair" open={completeOpen}
        onOk={handleComplete} onCancel={() => { setCompleteOpen(false); completeForm.resetFields(); }}
        destroyOnClose>
        <Form form={completeForm} layout="vertical">
          <Form.Item name="repairAction" label="Repair Action" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="repairCost" label="Repair Cost (¥)">
            <InputNumber style={{ width: '100%' }} min={0} precision={2} />
          </Form.Item>
          <Form.Item name="repairedBy" label="Repaired By"><Input /></Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default EquipmentRepairs;
