import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, InputNumber, Select, App, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';
import { getAnalysisItems, createAnalysisItem, updateAnalysisItem, deleteAnalysisItem } from '@/services/requestService';

const AnalysisItemList: React.FC = () => {
  const actionRef = useRef<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<any>(null);
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const intl = useIntl();

  const columns: ProColumns[] = [
    { title: 'Name', dataIndex: 'name', width: 200 },
    { title: 'Code', dataIndex: 'code', width: 120 },
    { title: 'Group', dataIndex: 'groupId', width: 120, search: false },
    { title: 'Test Site', dataIndex: 'testSite', width: 120, search: false },
    { title: 'Type', dataIndex: 'analysisType', width: 120, search: false },
    { title: 'Cost (¥)', dataIndex: 'cost', width: 100, search: false, render: (v: number) => v?.toFixed(2) || '-' },
    { title: 'Duration (Days)', dataIndex: 'durationDays', width: 130, search: false },
    {
      title: 'Active', dataIndex: 'active', width: 80, search: false,
      render: (v: boolean) => <Tag color={v ? 'green' : 'red'}>{v ? 'Yes' : 'No'}</Tag>,
    },
    {
      title: intl.formatMessage({ id: 'common.operation' }),
      valueType: 'option', width: 150,
      render: (_, record: any) => [
        <a key="edit" onClick={() => { setEditingRecord(record); form.setFieldsValue(record); setModalVisible(true); }}>Edit</a>,
        <a key="delete" onClick={() => {
          Modal.confirm({
            title: `Delete "${record.name}"?`,
            onOk: async () => { await deleteAnalysisItem(record.id); message.success('Deleted'); actionRef.current?.reload(); },
          });
        }}>Delete</a>,
      ],
    },
  ];

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingRecord?.id) {
      await updateAnalysisItem(editingRecord.id, values);
    } else {
      await createAnalysisItem(values);
    }
    message.success(intl.formatMessage({ id: 'common.success' }));
    setModalVisible(false); form.resetFields(); setEditingRecord(null);
    actionRef.current?.reload();
  };

  return (
    <>
      <ProTable
        columns={columns} actionRef={actionRef}
        request={async (params) => {
          try {
            const result = await getAnalysisItems({ page: params.current, size: params.pageSize });
            return { data: result?.data?.records ?? [], total: result?.data?.total ?? 0, success: result?.code === 200 };
          } catch {
            return { data: [], total: 0, success: false };
          }
        }}
        rowKey="id" search={{ labelWidth: 'auto' }}
        toolBarRender={() => [
          <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
            {intl.formatMessage({ id: 'common.create' })}
          </Button>,
        ]}
      />
      <Modal
        title={editingRecord ? 'Edit Analysis Item' : 'Create Analysis Item'}
        open={modalVisible} onOk={handleSubmit}
        onCancel={() => { setModalVisible(false); form.resetFields(); setEditingRecord(null); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="code" label="Code" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="groupId" label="Test Group"><Input /></Form.Item>
          <Form.Item name="testSite" label="Test Site"><Input /></Form.Item>
          <Form.Item name="analysisType" label="Analysis Type"><Input /></Form.Item>
          <Form.Item name="cost" label="Cost (¥)"><InputNumber min={0} precision={2} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="durationDays" label="Duration (Days)"><InputNumber min={1} style={{ width: '100%' }} /></Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default AnalysisItemList;
