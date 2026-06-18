import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, InputNumber, App, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';
import { getRequestTypes, createRequestType, updateRequestType, deleteRequestType } from '@/services/requestService';

const RequestTypeList: React.FC = () => {
  const actionRef = useRef<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<any>(null);
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const intl = useIntl();

  const columns: ProColumns[] = [
    { title: 'Name', dataIndex: 'name', width: 180 },
    { title: 'Code', dataIndex: 'code', width: 120 },
    { title: 'Duration (Days)', dataIndex: 'taskDurationDays', width: 130, search: false },
    { title: 'Description', dataIndex: 'description', ellipsis: true, search: false },
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
            onOk: async () => { await deleteRequestType(record.id); message.success('Deleted'); actionRef.current?.reload(); },
          });
        }}>Delete</a>,
      ],
    },
  ];

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingRecord?.id) {
      await updateRequestType(editingRecord.id, values);
    } else {
      await createRequestType(values);
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
            const result = await getRequestTypes({ page: params.current, size: params.pageSize });
            return { data: result?.data?.records ?? [], total: result?.data?.total ?? 0, success: result?.code === 200 };
          } catch (e: any) {
          message.error(e?.message || 'Load failed');
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
        title={editingRecord ? 'Edit Request Type' : 'Create Request Type'}
        open={modalVisible} onOk={handleSubmit}
        onCancel={() => { setModalVisible(false); form.resetFields(); setEditingRecord(null); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="code" label="Code" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="taskDurationDays" label="Duration (Days)"><InputNumber min={1} /></Form.Item>
          <Form.Item name="description" label="Description"><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default RequestTypeList;
