import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Tag, Button, Modal, Form, Select, Switch, App } from 'antd';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';
import { getAdminUsers, updateUserRoles, toggleUserActive } from '@/services/requestService';

const roleOptions = [
  { value: 'REQUESTER', label: 'Requester' },
  { value: 'TECHNICIAN', label: 'Technician' },
  { value: 'ENGINEER', label: 'Engineer' },
  { value: 'MANAGER', label: 'Manager' },
  { value: 'ADMIN', label: 'Admin' },
];

const UserList: React.FC = () => {
  const actionRef = useRef<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [editingUser, setEditingUser] = useState<API.SysUser | null>(null);
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const intl = useIntl();

  const columns: ProColumns<API.SysUser>[] = [
    { title: 'Display Name', dataIndex: 'displayName', width: 160 },
    { title: 'Email', dataIndex: 'email', width: 220 },
    { title: 'Login ID', dataIndex: 'loginId', width: 120, search: false },
    {
      title: 'Roles', dataIndex: 'roles', width: 200, search: false,
      render: (v: string) => v?.split(',').map((r: string) => <Tag key={r} color="blue">{r}</Tag>),
    },
    {
      title: 'Active', dataIndex: 'isActive', width: 80, search: false,
      render: (v: boolean, record: API.SysUser) => (
        <Switch
          checked={v}
          size="small"
          onChange={async () => {
            await toggleUserActive(record.id);
            message.success('Updated');
            actionRef.current?.reload();
          }}
        />
      ),
    },
    {
      title: 'Last Login', dataIndex: 'lastLoginAt', width: 160, valueType: 'dateTime', search: false,
    },
    {
      title: intl.formatMessage({ id: 'common.operation' }),
      valueType: 'option', width: 80,
      render: (_, record: API.SysUser) => [
        <a key="edit" onClick={() => { setEditingUser(record); form.setFieldsValue({ roles: record.roles?.split(',') || [] }); setModalVisible(true); }}>Edit Roles</a>,
      ],
    },
  ];

  const handleSubmit = async () => {
    const values = await form.validateFields();
    if (editingUser?.id) {
      await updateUserRoles(editingUser.id, { roles: values.roles.join(',') });
      message.success('Roles updated');
    }
    setModalVisible(false); form.resetFields(); setEditingUser(null);
    actionRef.current?.reload();
  };

  return (
    <>
      <ProTable<API.SysUser>
        columns={columns} actionRef={actionRef}
        request={async (params) => {
          try {
            const result = await getAdminUsers({ page: params.current, size: params.pageSize, keyword: params.keyword });
            return { data: result?.data?.records ?? [], total: result?.data?.total ?? 0, success: result?.code === 200 };
          } catch {
            return { data: [], total: 0, success: false };
          }
        }}
        rowKey="id" search={{ labelWidth: 'auto' }}
      />
      <Modal
        title="Edit User Roles" open={modalVisible} onOk={handleSubmit}
        onCancel={() => { setModalVisible(false); form.resetFields(); setEditingUser(null); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="roles" label="Roles" rules={[{ required: true }]}>
            <Select mode="multiple" options={roleOptions} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default UserList;
