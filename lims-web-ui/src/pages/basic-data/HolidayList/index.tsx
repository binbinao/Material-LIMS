import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, DatePicker, Select, App, Tag } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';
import { getHolidays, createHoliday, deleteHoliday } from '@/services/requestService';
import dayjs from 'dayjs';

const HolidayList: React.FC = () => {
  const actionRef = useRef<any>();
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const intl = useIntl();

  const holidayTypes = [
    { value: 'NATIONAL', label: intl.formatMessage({ id: 'basicData.holiday.type.national', defaultMessage: 'National' }) },
    { value: 'COMPANY', label: intl.formatMessage({ id: 'basicData.holiday.type.company', defaultMessage: 'Company' }) },
    { value: 'MAKEUP', label: intl.formatMessage({ id: 'basicData.holiday.type.makeup', defaultMessage: 'Makeup Work' }) },
  ];

  const holidayColor: Record<string, string> = { NATIONAL: 'red', COMPANY: 'blue', MAKEUP: 'green' };

  const getTypeLabel = (t: string) => {
    const ht = holidayTypes.find((h) => h.value === t);
    return ht?.label || t;
  };

  const columns: ProColumns[] = [
    { title: intl.formatMessage({ id: 'common.status' }), dataIndex: 'date', width: 130, render: (v: string) => dayjs(v).format('YYYY-MM-DD') },
    { title: intl.formatMessage({ id: 'equipment.name' }), dataIndex: 'name', width: 200 },
    {
      title: intl.formatMessage({ id: 'common.status' }), dataIndex: 'type', width: 120,
      valueType: 'select',
      valueEnum: Object.fromEntries(holidayTypes.map(({ value, label }) => [value, { text: label }])),
      render: (_, record: any) => <Tag color={holidayColor[record.type]}>{getTypeLabel(record.type)}</Tag>,
    },
    {
      title: intl.formatMessage({ id: 'common.operation' }),
      valueType: 'option', width: 100,
      render: (_, record: any) => [
        <a key="delete" onClick={() => {
          Modal.confirm({
            title: `${intl.formatMessage({ id: 'common.delete' })} "${record.name}"?`,
            onOk: async () => { await deleteHoliday(record.id); message.success(intl.formatMessage({ id: 'common.success' })); actionRef.current?.reload(); },
          });
        }}>{intl.formatMessage({ id: 'common.delete' })}</a>,
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
          } catch (e: any) {
          message.error(e?.message || intl.formatMessage({ id: 'common.fail' }));
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
        title={intl.formatMessage({ id: 'basicData.holiday.add' })} open={modalVisible} onOk={handleSubmit}
        onCancel={() => { setModalVisible(false); form.resetFields(); }}
      >
        <Form form={form} layout="vertical">
          <Form.Item name="date" label={intl.formatMessage({ id: 'common.status' })} rules={[{ required: true }]}><DatePicker style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="name" label={intl.formatMessage({ id: 'equipment.name' })} rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="type" label={intl.formatMessage({ id: 'common.status' })} rules={[{ required: true }]}>
            <Select options={holidayTypes} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default HolidayList;
