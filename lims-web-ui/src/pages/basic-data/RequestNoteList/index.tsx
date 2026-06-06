import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, App } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';

const RequestNoteList: React.FC = () => {
  const actionRef = useRef<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const intl = useIntl();

  const columns: ProColumns[] = [
    { title: 'Title', dataIndex: 'title', width: 250 },
    { title: 'Content', dataIndex: 'content', ellipsis: true, search: false },
    { title: 'Created By', dataIndex: 'createdBy', width: 120, search: false },
    { title: 'Created', dataIndex: 'createdAt', width: 150, valueType: 'dateTime', search: false },
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

export default RequestNoteList;
