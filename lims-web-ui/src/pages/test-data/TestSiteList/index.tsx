import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, App } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';

const TestSiteList: React.FC = () => {
  const actionRef = useRef<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const intl = useIntl();

  const columns: ProColumns[] = [
    { title: 'Name', dataIndex: 'name', width: 200 },
    { title: 'Code', dataIndex: 'code', width: 120 },
    { title: 'Description', dataIndex: 'description', ellipsis: true, search: false },
    {
      title: intl.formatMessage({ id: 'common.operation' }),
      valueType: 'option', width: 150,
      render: (_, record: any) => [<a key="edit">Edit</a>, <a key="delete">Delete</a>],
    },
  ];

  return (
    <ProTable
      columns={columns} actionRef={actionRef}
      request={async () => ({ data: [], total: 0, success: true })}
      rowKey="id" search={{ labelWidth: 'auto' }}
      toolBarRender={() => [
        <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
          {intl.formatMessage({ id: 'common.create' })}
        </Button>,
      ]}
    />
  );
};

export default TestSiteList;
