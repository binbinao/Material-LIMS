import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, InputNumber, App } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';
import { getBrands, createBrand, updateBrand, deleteBrand } from '@/services/requestService';

const BrandList: React.FC = () => {
  const actionRef = useRef<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<API.Brand | null>(null);
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const intl = useIntl();

  const columns: ProColumns<API.Brand>[] = [
    { title: 'Name', dataIndex: 'name', width: 200 },
    { title: 'Description', dataIndex: 'description', ellipsis: true, search: false },
    { title: 'Sort Order', dataIndex: 'sortOrder', width: 100, search: false },
    {
      title: intl.formatMessage({ id: 'common.operation' }),
      valueType: 'option',
      width: 150,
      render: (_, record) => [
        <a key="edit" onClick={() => handleEdit(record)}>Edit</a>,
        <a key="delete" onClick={() => handleDelete(record)}>Delete</a>,
      ],
    },
  ];

  const handleEdit = (record: API.Brand) => {
    setEditingRecord(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleDelete = async (record: API.Brand) => {
    Modal.confirm({
      title: `Delete brand "${record.name}"?`,
      onOk: async () => {
        await deleteBrand(record.id!);
        message.success(intl.formatMessage({ id: 'common.success' }));
        actionRef.current?.reload();
      },
    });
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingRecord?.id) {
      await updateBrand(editingRecord.id, values);
    } else {
      await createBrand(values);
    }
    message.success(intl.formatMessage({ id: 'common.success' }));
    setModalVisible(false);
    form.resetFields();
    setEditingRecord(null);
    actionRef.current?.reload();
  };

  return (
    <>
      <ProTable<API.Brand>
        columns={columns}
        actionRef={actionRef}
        request={async (params) => {
          try {
            const result = await getBrands({ page: params.current, size: params.pageSize });
            return {
              data: result?.data?.records ?? [],
              total: result?.data?.total ?? 0,
              success: result?.code === 200,
            };
          } catch {
            return { data: [], total: 0, success: false };
          }
        }}
        rowKey="id"
        search={{ labelWidth: 'auto' }}
        toolBarRender={() => [
          <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
            {intl.formatMessage({ id: 'common.create' })}
          </Button>,
        ]}
      />

      <Modal
        title={editingRecord ? 'Edit Brand' : 'Create Brand'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => { setModalVisible(false); form.resetFields(); setEditingRecord(null); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Brand Name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="sortOrder" label="Sort Order">
            <InputNumber min={0} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default BrandList;
