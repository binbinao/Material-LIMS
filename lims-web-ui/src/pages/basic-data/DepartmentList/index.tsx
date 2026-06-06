import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, InputNumber, TreeSelect, App } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';
import { getDepartments, createDepartment, updateDepartment, deleteDepartment } from '@/services/requestService';

const DepartmentList: React.FC = () => {
  const actionRef = useRef<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [editingRecord, setEditingRecord] = useState<any>(null);
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const intl = useIntl();
  const [deptData, setDeptData] = useState<any[]>([]);

  const buildTree = (items: any[], parentId?: string): any[] => {
    return items
      .filter((i) => (parentId ? i.parentId === parentId : !i.parentId))
      .map((i) => ({
        title: i.name,
        value: i.id,
        key: i.id,
        children: buildTree(items, i.id),
      }));
  };

  const columns: ProColumns[] = [
    { title: 'Name', dataIndex: 'name', width: 200 },
    { title: 'External ID', dataIndex: 'externalId', width: 120, search: false },
    { title: 'Level', dataIndex: 'level', width: 80, search: false },
    {
      title: intl.formatMessage({ id: 'common.operation' }),
      valueType: 'option', width: 150,
      render: (_, record: any) => [
        <a key="edit" onClick={() => { setEditingRecord(record); form.setFieldsValue(record); setModalVisible(true); }}>Edit</a>,
        <a key="delete" onClick={() => {
          Modal.confirm({
            title: `Delete "${record.name}"?`,
            onOk: async () => { await deleteDepartment(record.id); message.success('Deleted'); actionRef.current?.reload(); },
          });
        }}>Delete</a>,
      ],
    },
  ];

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingRecord?.id) {
      await updateDepartment(editingRecord.id, values);
    } else {
      await createDepartment(values);
    }
    message.success(intl.formatMessage({ id: 'common.success' }));
    setModalVisible(false); form.resetFields(); setEditingRecord(null);
    actionRef.current?.reload();
  };

  return (
    <>
      <ProTable
        columns={columns} actionRef={actionRef}
        request={async () => {
          const result = await getDepartments();
          const items = result?.data ?? [];
          setDeptData(items);
          return { data: items, total: items.length, success: result?.code === 200 };
        }}
        rowKey="id" search={false}
        toolBarRender={() => [
          <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
            {intl.formatMessage({ id: 'common.create' })}
          </Button>,
        ]}
      />
      <Modal
        title={editingRecord ? 'Edit Department' : 'Create Department'}
        open={modalVisible} onOk={handleSubmit}
        onCancel={() => { setModalVisible(false); form.resetFields(); setEditingRecord(null); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="parentId" label="Parent Department">
            <TreeSelect
              treeData={buildTree(deptData)}
              placeholder="Top-level department"
              allowClear
              treeDefaultExpandAll
            />
          </Form.Item>
          <Form.Item name="sortOrder" label="Sort Order"><InputNumber min={0} /></Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default DepartmentList;
