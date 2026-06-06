import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, DatePicker, Select, App, Tag, Upload } from 'antd';
import { PlusOutlined, UploadOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';
import { getHolidays, createHoliday, deleteHoliday } from '@/services/requestService';
import dayjs from 'dayjs';

const holidayTypeMap: Record<string, { color: string; text: string }> = {
  NATIONAL: { color: 'red', text: 'National' },
  COMPANY: { color: 'blue', text: 'Company' },
  MAKEUP: { color: 'green', text: 'Makeup Work' },
};

const HolidayList: React.FC = () => {
  const actionRef = useRef<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const intl = useIntl();

  const columns: ProColumns[] = [
    {
      title: 'Date', dataIndex: 'date', width: 130,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD'),
    },
    { title: 'Name', dataIndex: 'name', width: 200 },
    {
      title: 'Type', dataIndex: 'type', width: 120,
      valueType: 'select',
      valueEnum: Object.fromEntries(Object.entries(holidayTypeMap).map(([k, v]) => [k, { text: v.text }])),
      render: (_, record: any) => <Tag color={holidayTypeMap[record.type]?.color}>{holidayTypeMap[record.type]?.text || record.type}</Tag>,
    },
    {
      title: intl.formatMessage({ id: 'common.operation' }),
      valueType: 'option', width: 100,
      render: (_, record: any) => [
        <a key="delete" onClick={() => {
          Modal.confirm({
            title: `Delete "${record.name}"?`,
            onOk: async () => { await deleteHoliday(record.id); message.success('Deleted'); actionRef.current?.reload(); },
          });
        }}>Delete</a>,
      ],
    },
  ];

  const handleSubmit = async () => {
    const values = await form.validateFields();
    values.date = values.date?.format('YYYY-MM-DD');
    await createHoliday(values);
    message.success(intl.formatMessage({ id: 'common.success' }));
    setModalVisible(false); form.resetFields();
    actionRef.current?.reload();
  };

  return (
    <>
      <ProTable
        columns={columns} actionRef={actionRef}
        request={async (params) => {
          try {
            const result = await getHolidays({ page: params.current, size: params.pageSize, type: params.type });
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
        title="Add Holiday" open={modalVisible} onOk={handleSubmit}
        onCancel={() => { setModalVisible(false); form.resetFields(); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="date" label="Date" rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="name" label="Holiday Name" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="type" label="Type" rules={[{ required: true }]}>
            <Select options={Object.entries(holidayTypeMap).map(([k, v]) => ({ value: k, label: v.text }))} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default HolidayList;
