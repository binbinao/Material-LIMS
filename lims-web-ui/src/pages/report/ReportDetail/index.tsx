import { PageContainer } from '@ant-design/pro-components';
import { Card, Descriptions, Tag, Button, Space, App, Modal, Form, Input } from 'antd';
import { useParams, history, useAccess, useModel, useIntl } from '@umijs/max';
import { useRequest } from 'ahooks';
import { getReport, submitReport, approveReport, rejectReport, reviseReport, getReportEditUrl, syncReportFromSharePoint } from '@/services/requestService';
import dayjs from 'dayjs';

const reportStatusColor: Record<string, string> = {
  DRAFT: 'default',
  IN_REVIEW: 'processing',
  APPROVED: 'success',
  REVISING: 'warning',
};

const ReportDetail: React.FC = () => {
  const params = useParams<{ id: string }>();
  const { message, modal } = App.useApp();
  const intl = useIntl();

  const [reviseForm] = Form.useForm();

  const access = useAccess();
  const { initialState } = useModel('@@initialState');
  const currentUserId = initialState?.currentUser?.id;

  const { data: reportData, loading, refresh } = useRequest(() => getReport(params.id));
  const report = reportData?.data;

  const getReportStatusText = (s: string) =>
    intl.formatMessage({ id: `report.status.${s}`, defaultMessage: s });

  const isAuthor = !!report && !!currentUserId && report.authorId === currentUserId;

  const handleAction = async (action: string) => {
    try {
      switch (action) {
        case 'submit':
          await submitReport(params.id);
          message.success(intl.formatMessage({ id: 'common.success' }));
          break;
        case 'approve':
          await approveReport(params.id);
          message.success(intl.formatMessage({ id: 'common.success' }));
          break;
        case 'reject':
          await rejectReport(params.id);
          message.success(intl.formatMessage({ id: 'common.success' }));
          break;
        case 'revise':
          modal.confirm({
            title: intl.formatMessage({ id: 'report.label.revisionNote', defaultMessage: 'Revise Report' }),
            content: (
              <Form form={reviseForm} layout="vertical">
                <Form.Item name="revisionNote" label={intl.formatMessage({ id: 'report.label.revisionNote', defaultMessage: 'Revision Note' })} rules={[{ required: true }]}>
                  <Input.TextArea rows={3} />
                </Form.Item>
              </Form>
            ),
            onOk: async () => {
              try {
                const values = await reviseForm.validateFields();
                await reviseReport(params.id, { revisionNote: values.revisionNote });
                reviseForm.resetFields();
                message.success(intl.formatMessage({ id: 'common.success' }));
                refresh();
              } catch {
                throw new Error('validation');
              }
            },
            onCancel: () => reviseForm.resetFields(),
          });
          return;
        case 'edit':
          const editRes = await getReportEditUrl(params.id);
          if (editRes?.data) {
            window.open(editRes.data, '_blank');
          }
          return;
        case 'sync':
          await syncReportFromSharePoint(params.id);
          message.success(intl.formatMessage({ id: 'common.success' }));
          break;
      }
      refresh();
    } catch {
      message.error(intl.formatMessage({ id: 'common.fail' }));
    }
  };

  const actionButtons = () => {
    if (!report) return null;
    const btns: React.ReactNode[] = [];
    switch (report.status) {
      case 'DRAFT':
      case 'REVISING':
        if ((access.canEngineer || access.canManager) && isAuthor) {
          btns.push(
            <Button key="edit" onClick={() => handleAction('edit')}>{intl.formatMessage({ id: 'common.edit' })}</Button>,
            <Button key="sync" onClick={() => handleAction('sync')}>{intl.formatMessage({ id: 'common.download' })}</Button>,
            <Button key="submit" type="primary" onClick={() => handleAction('submit')}>{intl.formatMessage({ id: 'common.submit' })}</Button>,
          );
        }
        break;
      case 'IN_REVIEW':
        if (access.canManager) {
          btns.push(
            <Button key="approve" type="primary" onClick={() => handleAction('approve')}>{intl.formatMessage({ id: 'common.confirm' })}</Button>,
            <Button key="reject" danger onClick={() => handleAction('reject')}>{intl.formatMessage({ id: 'common.cancel' })}</Button>,
          );
        }
        break;
      case 'APPROVED':
        if (access.canEngineer || access.canManager) {
          btns.push(
            <Button key="revise" type="primary" onClick={() => handleAction('revise')}>{intl.formatMessage({ id: 'common.edit' })}</Button>,
          );
        }
        break;
    }
    return btns.length > 0 ? <Space>{btns}</Space> : null;
  };

  return (
    <PageContainer
      title={`${intl.formatMessage({ id: 'report.detail.document' })} ${report?.versionNumber || ''}`}
      onBack={() => history.push('/report/list')}
      extra={actionButtons()}
    >
      {report && (
        <>
          <Card style={{ marginBottom: 16 }}>
            <Descriptions column={3}>
              <Descriptions.Item label={intl.formatMessage({ id: 'report.label.version', defaultMessage: 'Version' })}>{report.versionNumber}</Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'report.label.status', defaultMessage: 'Status' })}>
                <Tag color={reportStatusColor[report.status]}>
                  {getReportStatusText(report.status)}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'report.label.author', defaultMessage: 'Author' })}>{report.authorId}</Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'report.label.requestId', defaultMessage: 'Request ID' })}>
                <a onClick={() => history.push(`/request/${report.requestId}`)}>{report.requestId.substring(0, 8)}...</a>
              </Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'report.label.submitted', defaultMessage: 'Submitted' })}>
                {report.submittedAt ? dayjs(report.submittedAt).format('YYYY-MM-DD HH:mm') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'report.label.approved', defaultMessage: 'Approved' })}>
                {report.approvedAt ? dayjs(report.approvedAt).format('YYYY-MM-DD HH:mm') : '-'}
              </Descriptions.Item>
              {report.revisionNote && (
                <Descriptions.Item label={intl.formatMessage({ id: 'report.label.revisionNote', defaultMessage: 'Revision Note' })} span={3}>{report.revisionNote}</Descriptions.Item>
              )}
            </Descriptions>
          </Card>

          <Card title={intl.formatMessage({ id: 'report.detail.document' })} style={{ marginBottom: 16 }}>
            <Space>
              <Button type="link" href={`/api/v1/reports/${report.id}/sample-word`} target="_blank">{intl.formatMessage({ id: 'common.download' })} Word</Button>
              {report.pdfUrl && <Button type="link" href={report.pdfUrl} target="_blank">{intl.formatMessage({ id: 'common.download' })} PDF</Button>}
            </Space>
          </Card>

          <Card title={intl.formatMessage({ id: 'report.detail.versionHistory' })} extra={<Button onClick={() => history.push(`/report/${report.id}/revisions`)}>{intl.formatMessage({ id: 'report.detail.viewAllRevisions' })}</Button>}>
            <span>{intl.formatMessage({ id: 'report.label.version', defaultMessage: 'Version' })}: {report.versionNumber}</span>
          </Card>
        </>
      )}
    </PageContainer>
  );
};

export default ReportDetail;