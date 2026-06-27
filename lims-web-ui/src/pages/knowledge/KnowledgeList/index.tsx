import type { ProColumns } from '@ant-design/pro-components';
import { ProTable } from '@ant-design/pro-components';
import { Button, Modal, Form, Input, Select, Upload, App, Popconfirm, Tag } from 'antd';
import { PlusOutlined, UploadOutlined, DownloadOutlined } from '@ant-design/icons';
import { useRef, useState } from 'react';
import { useIntl } from '@umijs/max';
import { getKnowledgeDocs, uploadKnowledgeDoc, deleteKnowledgeDoc } from '@/services/requestService';

const CATEGORY_COLOR: Record<string, string> = { MANUAL: 'blue', VIDEO: 'purple' };

const KnowledgeList: React.FC = () => {
  const actionRef = useRef<any>();
  const [open, setOpen] = useState(false);
  const [form] = Form.useForm();
  const [fileList, setFileList] = useState<any[]>([]);
  const { message } = App.useApp();
  const intl = useIntl();

  const handleUpload = async () => {
    const v = await form.validateFields();
    if (!fileList.length) {
      message.error(intl.formatMessage({ id: 'common.fail' }));
      return;
    }
    const fd = new FormData();
    fd.append('file', fileList[0].originFileObj);
    fd.append('title', v.title);
    fd.append('category', v.category);
    if (v.description) fd.append('description', v.description);
    try {
      await uploadKnowledgeDoc(fd);
      message.success(intl.formatMessage({ id: 'common.success' }));
      setOpen(false); form.resetFields(); setFileList([]);
      actionRef.current?.reload();
    } catch (e: any) {
      message.error(e?.message || intl.formatMessage({ id: 'common.fail' }));
    }
  };

  const columns: ProColumns[] = [
    {
      title: intl.formatMessage({ id: 'knowledge.title' }), dataIndex: 'title', width: 320,
      render: (_, r: any) => <a href={r.fileUrl} target="_blank" rel="noreferrer">{r.title}</a>,
    },
    {
      title: intl.formatMessage({ id: 'knowledge.category' }), dataIndex: 'category', width: 110,
      valueType: 'select',
      valueEnum: { MANUAL: { text: intl.formatMessage({ id: 'knowledge.category.MANUAL' }) }, VIDEO: { text: intl.formatMessage({ id: 'knowledge.category.VIDEO' }) } },
      render: (_, r: any) => <Tag color={CATEGORY_COLOR[r.category]}>{intl.formatMessage({ id: `knowledge.category.${r.category}`, defaultMessage: r.category })}</Tag>,
    },
    {
      title: intl.formatMessage({ id: 'knowledge.size' }), dataIndex: 'fileSize', width: 100, search: false,
      render: (v: number) => v ? `${(v / 1024 / 1024).toFixed(2)} MB` : '-',
    },
    { title: intl.formatMessage({ id: 'knowledge.description' }), dataIndex: 'description', search: false, ellipsis: true },
    { title: intl.formatMessage({ id: 'common.operation' }), dataIndex: 'updatedAt', width: 160, valueType: 'dateTime', search: false },
    {
      title: intl.formatMessage({ id: 'common.operation' }), valueType: 'option', width: 160, fixed: 'right',
      render: (_, r: any) => [
        <a key="dl" href={r.fileUrl} target="_blank" rel="noreferrer">
          <DownloadOutlined /> {intl.formatMessage({ id: 'common.download' })}
        </a>,
        <Popconfirm key="del" title={intl.formatMessage({ id: 'knowledge.delete.confirm' })} onConfirm={async () => {
          await deleteKnowledgeDoc(r.id);
          message.success(intl.formatMessage({ id: 'common.success' }));
          actionRef.current?.reload();
        }}>
          <a style={{ color: '#f5222d' }}>{intl.formatMessage({ id: 'common.delete' })}</a>
        </Popconfirm>,
      ],
    },
  ];

  return (
    <>
      <ProTable
        columns={columns}
        actionRef={actionRef}
        rowKey="id"
        scroll={{ x: 1000 }}
        request={async (params) => {
          try {
            const r = await getKnowledgeDocs({
              page: (params.current || 1) - 1,
              size: params.pageSize,
              category: params.category,
              keyword: params.title,
            });
            return { data: r?.data?.records ?? [], total: r?.data?.total ?? 0, success: r?.code === 200 };
          } catch {
            return { data: [], total: 0, success: false };
          }
        }}
        search={{ labelWidth: 'auto' }}
        toolBarRender={() => [
          <Button key="up" type="primary" icon={<PlusOutlined />} onClick={() => setOpen(true)}>{intl.formatMessage({ id: 'knowledge.upload' })}</Button>,
        ]}
      />
      <Modal title={intl.formatMessage({ id: 'knowledge.upload.title' })} open={open}
        onOk={handleUpload} onCancel={() => { setOpen(false); form.resetFields(); setFileList([]); }}
        destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="title" label={intl.formatMessage({ id: 'knowledge.title' })} rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="category" label={intl.formatMessage({ id: 'knowledge.category' })} rules={[{ required: true }]}>
            <Select options={[
              { value: 'MANUAL', label: intl.formatMessage({ id: 'knowledge.category.MANUAL' }) },
              { value: 'VIDEO', label: intl.formatMessage({ id: 'knowledge.category.VIDEO' }) },
            ]} />
          </Form.Item>
          <Form.Item label={intl.formatMessage({ id: 'knowledge.upload' })} required>
            <Upload
              fileList={fileList}
              beforeUpload={() => false}
              maxCount={1}
              onChange={({ fileList: fl }) => setFileList(fl)}>
              <Button icon={<UploadOutlined />}>{intl.formatMessage({ id: 'common.upload' })}</Button>
            </Upload>
          </Form.Item>
          <Form.Item name="description" label={intl.formatMessage({ id: 'knowledge.description' })}><Input.TextArea rows={3} /></Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default KnowledgeList;
