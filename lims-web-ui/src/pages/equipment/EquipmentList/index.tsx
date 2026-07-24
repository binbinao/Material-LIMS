import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, Select, DatePicker, Tag, App } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { request, useIntl } from '@umijs/max';

const statusColors: Record<string, string> = { ACTIVE: 'green', UNDER_REPAIR: 'orange', DECOMMISSIONED: 'red' };

const EquipmentList: React.FC = () => {
  const intl = useIntl();
  const actionRef = useRef<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<any>(null);
  const [form] = Form.useForm();
  const { message } = App.useApp();

  const t = (id: string, defaultMessage: string) => intl.formatMessage({ id, defaultMessage });

  const columns: ProColumns<any>[] = [
    { title: t('equipment.name', 'Name'), dataIndex: 'name', width: 180 },
    { title: t('equipment.model', 'Model'), dataIndex: 'model', width: 120 },
    { title: t('equipment.serialNumber', 'Serial No.'), dataIndex: 'serialNumber', width: 130 },
    { title: t('equipment.status', 'Status'), dataIndex: 'status', width: 100, render: (_, r) => <Tag color={statusColors[r.status]}>{r.status}</Tag> },
    { title: t('equipment.location', 'Location'), dataIndex: 'location', width: 150, search: false },
    { title: t('equipment.purchaseDate', 'Purchase Date'), dataIndex: 'purchaseDate', valueType: 'date', width: 120, search: false },
    { title: t('equipment.warrantyExpiry', 'Warranty Expiry'), dataIndex: 'warrantyExpiry', valueType: 'date', width: 120, search: false },
    {
      title: t('common.action', 'Action'), valueType: 'option', width: 120,
      render: (_, record) => [
        <a key="edit" role="button" aria-label={t('common.edit', 'Edit')} onClick={() => { setEditingRecord(record); form.setFieldsValue(record); setModalVisible(true); }}>{t('common.edit', 'Edit')}</a>,
        <a key="status" role="button" aria-label={t('equipment.changeStatus', 'Change Status')} onClick={() => handleStatusChange(record)}>{t('equipment.changeStatus', 'Change Status')}</a>,
      ],
    },
  ];

  const handleStatusChange = async (record: any) => {
    const nextStatus = record.status === 'ACTIVE' ? 'UNDER_REPAIR' : record.status === 'UNDER_REPAIR' ? 'DECOMMISSIONED' : 'ACTIVE';
    await request(`/api/v1/equipments/${record.id}/status`, { method: 'PATCH', data: { status: nextStatus } });
    message.success('Status updated');
    actionRef.current?.reload();
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingRecord?.id) {
      await request(`/api/v1/equipments/${editingRecord.id}`, { method: 'PUT', data: values });
    } else {
      await request('/api/v1/equipments', { method: 'POST', data: values });
    }
    message.success('Success');
    setModalVisible(false);
    form.resetFields();
    setEditingRecord(null);
    actionRef.current?.reload();
  };

  return (
    <>
      <ProTable
        columns={columns}
        actionRef={actionRef}
        request={async (params) => {
          try {
            const res = await request('/api/v1/equipments', { params: { page: params.current, size: params.pageSize, status: params.status } });
            return { data: res?.data?.records ?? [], total: res?.data?.total ?? 0, success: res?.code === 200 };
          } catch (err) {
            message.error('Failed to load equipment list');
            return { data: [], total: 0, success: false };
          }
        }}
        rowKey="id"
        search={{ labelWidth: 'auto' }}
        toolBarRender={() => [<Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>Create</Button>]}
      />
      <Modal title={editingRecord ? 'Edit Equipment' : 'Create Equipment'} open={modalVisible} onOk={handleSubmit} onCancel={() => { setModalVisible(false); form.resetFields(); setEditingRecord(null); }}>
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="model" label="Model"><Input /></Form.Item>
          <Form.Item name="serialNumber" label="Serial Number"><Input /></Form.Item>
          <Form.Item name="status" label="Status" initialValue="ACTIVE"><Select options={[{ label: 'Active', value: 'ACTIVE' }, { label: 'Under Repair', value: 'UNDER_REPAIR' }, { label: 'Decommissioned', value: 'DECOMMISSIONED' }]} /></Form.Item>
          <Form.Item name="location" label="Location"><Input /></Form.Item>
          <Form.Item name="description" label="Description"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default EquipmentList;
