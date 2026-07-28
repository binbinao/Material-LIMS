import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, Select, InputNumber, App, Tag, Space, Popconfirm } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';
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
  const intl = useIntl();
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
      message.success(intl.formatMessage({ id: 'common.success' }));
      setCreateOpen(false);
      createForm.resetFields();
      actionRef.current?.reload();
    } catch (e: any) {
      message.error(e?.message || intl.formatMessage({ id: 'common.fail' }));
    }
  };

  const handleComplete = async () => {
    const values = await completeForm.validateFields();
    try {
      await completeEquipmentRepair(activeRecord.id, values);
      message.success(intl.formatMessage({ id: 'common.success' }));
      setCompleteOpen(false);
      completeForm.resetFields();
      actionRef.current?.reload();
    } catch (e: any) {
      message.error(e?.message || intl.formatMessage({ id: 'common.fail' }));
    }
  };

  const columns: ProColumns[] = [
    { title: intl.formatMessage({ id: 'repair.reportDate' }), dataIndex: 'reportDate', width: 120, valueType: 'date' },
    { title: intl.formatMessage({ id: 'equipment.name' }), dataIndex: 'equipmentId', width: 200, search: false },
    { title: intl.formatMessage({ id: 'repair.fault' }), dataIndex: 'faultDescription', ellipsis: true, search: false },
    { title: intl.formatMessage({ id: 'repair.reportedBy' }), dataIndex: 'reportedBy', width: 120, search: false },
    {
      title: intl.formatMessage({ id: 'common.status' }), dataIndex: 'status', width: 110,
      valueType: 'select',
      valueEnum: {
        REPORTING: { text: intl.formatMessage({ id: 'repair.status.REPORTING' }) },
        REPAIRING: { text: intl.formatMessage({ id: 'repair.status.REPAIRING' }) },
        COMPLETED: { text: intl.formatMessage({ id: 'repair.status.COMPLETED' }) },
      },
      render: (_: any, r: any) => <Tag color={STATUS_COLOR[r.status] || 'default'}>{intl.formatMessage({ id: `repair.status.${r.status}`, defaultMessage: r.status })}</Tag>,
    },
    { title: intl.formatMessage({ id: 'repair.cost' }), dataIndex: 'repairCost', width: 110, valueType: 'money', search: false },
    { title: intl.formatMessage({ id: 'repair.completionDate' }), dataIndex: 'completionDate', width: 120, valueType: 'date', search: false },
    {
      title: intl.formatMessage({ id: 'common.operation' }), valueType: 'option', width: 160, fixed: 'right',
      render: (_: any, r: any) => [
        r.status !== 'COMPLETED' && (
          <a key="complete" onClick={() => { setActiveRecord(r); setCompleteOpen(true); }}>{intl.formatMessage({ id: 'repair.complete' })}</a>
        ),
        <Popconfirm key="del" title={intl.formatMessage({ id: 'equipment.repairs.deleteConfirm' })} onConfirm={async () => {
          await deleteEquipmentRepair(r.id);
          message.success(intl.formatMessage({ id: 'common.success' }));
          actionRef.current?.reload();
        }}><a style={{ color: '#f5222d' }}>{intl.formatMessage({ id: 'common.delete' })}</a></Popconfirm>,
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
          message.error(e?.message || intl.formatMessage({ id: 'common.fail' }));
          return { data: [], total: 0, success: false };
        }
        }}
        search={{ labelWidth: 'auto' }}
        toolBarRender={() => [
          <Button key="add" type="primary" icon={<PlusOutlined />} onClick={() => { loadEquipments(); setCreateOpen(true); }}>
            {intl.formatMessage({ id: 'repair.report' })}
          </Button>,
        ]}
      />

      <Modal title={intl.formatMessage({ id: 'equipment.repairs.report.title' })} open={createOpen}
        onOk={handleCreate} onCancel={() => { setCreateOpen(false); createForm.resetFields(); }}
        destroyOnClose>
        <Form form={createForm} layout="vertical">
          <Form.Item name="equipmentId" label={intl.formatMessage({ id: 'equipment.name' })} rules={[{ required: true }]}>
            <Select showSearch options={equipmentOptions} placeholder={intl.formatMessage({ id: 'common.search' })} optionFilterProp="label" />
          </Form.Item>
          <Form.Item name="faultDescription" label={intl.formatMessage({ id: 'repair.fault' })} rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="reportedBy" label={intl.formatMessage({ id: 'repair.reportedBy' })}><Input /></Form.Item>
        </Form>
      </Modal>

      <Modal title={intl.formatMessage({ id: 'equipment.repairs.complete.title' })} open={completeOpen}
        onOk={handleComplete} onCancel={() => { setCompleteOpen(false); completeForm.resetFields(); }}
        destroyOnClose>
        <Form form={completeForm} layout="vertical">
          <Form.Item name="repairAction" label={intl.formatMessage({ id: 'repair.action' })} rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="repairCost" label={intl.formatMessage({ id: 'repair.cost' }) + ' (¥)'}>
            <InputNumber style={{ width: '100%' }} min={0} precision={2} />
          </Form.Item>
          <Form.Item name="repairedBy" label={intl.formatMessage({ id: 'repair.repairedBy' })}><Input /></Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default EquipmentRepairs;
