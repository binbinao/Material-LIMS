import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, Select, App, Popconfirm } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';
import { getI18nMessages, upsertI18nMessage, deleteI18nMessage } from '@/services/requestService';

interface IRow { messageKey: string; zhCN?: string; enUS?: string; }

/** Merge zh-CN + en-US two-locale dictionary into a single row. */
async function loadMerged(): Promise<Row[]> {
  const [zh, en] = await Promise.all([
    getI18nMessages('zh-CN').catch(() => ({ data: {} })),
    getI18nMessages('en-US').catch(() => ({ data: {} })),
  ]);
  const zhMap: Record<string, string> = zh?.data || {};
  const enMap: Record<string, string> = en?.data || {};
  const allKeys = new Set([...Object.keys(zhMap), ...Object.keys(enMap)]);
  return Array.from(allKeys).sort().map((k) => ({ messageKey: k, zhCN: zhMap[k], enUS: enMap[k] }));
}

const I18nList: React.FC = () => {
  const actionRef = useRef<any>();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Row | null>(null);
  const [form] = Form.useForm();
  const { message } = App.useApp();
  const intl = useIntl();

  const handleSave = async () => {
    const v = await form.validateFields();
    try {
      const tasks: Promise<any>[] = [];
      if (v.zhCN) tasks.push(upsertI18nMessage({ messageKey: v.messageKey, locale: 'zh-CN', messageValue: v.zhCN }));
      if (v.enUS) tasks.push(upsertI18nMessage({ messageKey: v.messageKey, locale: 'en-US', messageValue: v.enUS }));
      await Promise.all(tasks);
      message.success(intl.formatMessage({ id: 'common.success' }));
      setOpen(false); setEditing(null); form.resetFields();
      actionRef.current?.reload();
    } catch (e: any) {
      message.error(e?.message || 'Save failed');
    }
  };

  const columns: ProColumns<Row>[] = [
    { title: 'Key', dataIndex: 'messageKey', width: 280 },
    { title: 'zh-CN', dataIndex: 'zhCN', search: false },
    { title: 'en-US', dataIndex: 'enUS', search: false },
    {
      title: intl.formatMessage({ id: 'common.operation' }),
      valueType: 'option', width: 160, fixed: 'right',
      render: (_, r) => [
        <a key="e" onClick={() => {
          setEditing(r); form.setFieldsValue(r); setOpen(true);
        }}>{intl.formatMessage({ id: 'common.edit' })}</a>,
        <Popconfirm key="d" title="Delete both locales?" onConfirm={async () => {
          await Promise.allSettled([
            deleteI18nMessage({ messageKey: r.messageKey, locale: 'zh-CN' }),
            deleteI18nMessage({ messageKey: r.messageKey, locale: 'en-US' }),
          ]);
          message.success('Deleted');
          actionRef.current?.reload();
        }}><a style={{ color: '#f5222d' }}>{intl.formatMessage({ id: 'common.delete' })}</a></Popconfirm>,
      ],
    },
  ];

  return (
    <>
      <ProTable<Row>
        columns={columns}
        actionRef={actionRef}
        rowKey="messageKey"
        request={async (params) => {
          const all = await loadMerged();
          const keyword = (params.messageKey || '').toLowerCase();
          const filtered = keyword ? all.filter((r) => r.messageKey.toLowerCase().includes(keyword)) : all;
          return { data: filtered, total: filtered.length, success: true };
        }}
        search={{ labelWidth: 'auto' }}
        pagination={{ pageSize: 20 }}
        toolBarRender={() => [
          <Button key="c" type="primary" icon={<PlusOutlined />}
            onClick={() => { setEditing(null); form.resetFields(); setOpen(true); }}>
            {intl.formatMessage({ id: 'common.create' })}
          </Button>,
        ]}
      />
      <Modal title={editing ? 'Edit Translation' : 'Add Translation'} open={open}
        onOk={handleSave} onCancel={() => { setOpen(false); setEditing(null); form.resetFields(); }}
        destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="messageKey" label="Key" rules={[{ required: true }]}>
            <Input disabled={!!editing} />
          </Form.Item>
          <Form.Item name="zhCN" label="Chinese (zh-CN)"><Input /></Form.Item>
          <Form.Item name="enUS" label="English (en-US)"><Input /></Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default I18nList;
