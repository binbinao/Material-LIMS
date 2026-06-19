import { PageContainer } from '@ant-design/pro-components';
import { Card, Descriptions, Tag, Button, Space, App, Modal, Form, Input } from 'antd';
import { useParams, history, useAccess, useModel } from '@umijs/max';
import { useRequest } from 'ahooks';
import { getReport, submitReport, approveReport, rejectReport, reviseReport, getReportEditUrl, syncReportFromSharePoint } from '@/services/requestService';
import dayjs from 'dayjs';

const reportStatusMap: Record<string, { color: string; text: string }> = {
  DRAFT: { color: 'default', text: 'Draft' },
  IN_REVIEW: { color: 'processing', text: 'In Review' },
  APPROVED: { color: 'success', text: 'Approved' },
  REVISING: { color: 'warning', text: 'Revising' },
};

const ReportDetail: React.FC = () => {
  const params = useParams<{ id: string }>();
  const { message, modal } = App.useApp();

  // Issue #11: Form instance for the revise modal. Reading values via
  // document.getElementById bypassed Ant's <Form rules={[{ required: true }]}>
  // validation; useForm + validateFields() makes the required rule fire.
  const [reviseForm] = Form.useForm();

  // Issue #24: gate action buttons by role.
  const access = useAccess();
  const { initialState } = useModel('@@initialState');
  const currentUserId = initialState?.currentUser?.id;

  const { data: reportData, loading, refresh } = useRequest(() => getReport(params.id));
  const report = reportData?.data;

  // Issue #54 (P8): only the report's author may Submit / Edit / Sync
  // their own report. The backend's validateReportOwnership() rejects
  // non-authors with ACCESS_DENIED — without this UI gate, any engineer
  // or manager sees the Submit button and gets a confusing toast on click.
  const isAuthor = !!report && !!currentUserId && report.authorId === currentUserId;

  // Issue #54 (P8): only the report's author can Submit/Edit/Sync their own
  // report. The backend's validateReportOwnership() rejects non-authors
  // with ACCESS_DENIED; without this UI gate, any ENGINEER/MANAGER sees the
  // Submit button and gets a confusing toast on click.
  const currentUserId = useCurrentUserId();
  const isAuthor = !!report && !!currentUserId && report.authorId === currentUserId;

  const handleAction = async (action: string) => {
    try {
      switch (action) {
        case 'submit':
          await submitReport(params.id);
          message.success('Report submitted for review');
          break;
        case 'approve':
          await approveReport(params.id);
          message.success('Report approved');
          break;
        case 'reject':
          await rejectReport(params.id);
          message.success('Report rejected');
          break;
        case 'revise':
          modal.confirm({
            title: 'Revise Report',
            content: (
              <Form form={reviseForm} layout="vertical">
                <Form.Item name="revisionNote" label="Revision Note" rules={[{ required: true, message: 'Please enter a revision note' }]}>
                  <Input.TextArea rows={3} placeholder="What changed in this revision?" />
                </Form.Item>
              </Form>
            ),
            onOk: async () => {
              try {
                const values = await reviseForm.validateFields();
                await reviseReport(params.id, { revisionNote: values.revisionNote });
                reviseForm.resetFields();
                message.success('Report revision started');
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
          message.success('Report synced from SharePoint');
          break;
      }
      refresh();
    } catch {
      message.error('Action failed');
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
            <Button key="edit" onClick={() => handleAction('edit')}>Edit in M365</Button>,
            <Button key="sync" onClick={() => handleAction('sync')}>Sync from SharePoint</Button>,
            <Button key="submit" type="primary" onClick={() => handleAction('submit')}>Submit</Button>,
          );
        }
        break;
      case 'IN_REVIEW':
        if (access.canManager) {
          btns.push(
            <Button key="approve" type="primary" onClick={() => handleAction('approve')}>Approve</Button>,
            <Button key="reject" danger onClick={() => handleAction('reject')}>Reject</Button>,
          );
        }
        break;
      case 'APPROVED':
        if (access.canEngineer || access.canManager) {
          btns.push(
            <Button key="revise" type="primary" onClick={() => handleAction('revise')}>Revise</Button>,
          );
        }
        break;
    }
    return btns.length > 0 ? <Space>{btns}</Space> : null;
  };

  return (
    <PageContainer
      title={`Report ${report?.versionNumber || ''}`}
      onBack={() => history.push('/report/list')}
      extra={actionButtons()}
    >
      {report && (
        <>
          <Card style={{ marginBottom: 16 }}>
            <Descriptions column={3}>
              <Descriptions.Item label="Version">{report.versionNumber}</Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={reportStatusMap[report.status]?.color}>
                  {reportStatusMap[report.status]?.text || report.status}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Author">{report.authorId}</Descriptions.Item>
              <Descriptions.Item label="Request ID">
                <a onClick={() => history.push(`/request/${report.requestId}`)}>{report.requestId.substring(0, 8)}...</a>
              </Descriptions.Item>
              <Descriptions.Item label="Submitted">
                {report.submittedAt ? dayjs(report.submittedAt).format('YYYY-MM-DD HH:mm') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Approved">
                {report.approvedAt ? dayjs(report.approvedAt).format('YYYY-MM-DD HH:mm') : '-'}
              </Descriptions.Item>
              {report.revisionNote && (
                <Descriptions.Item label="Revision Note" span={3}>{report.revisionNote}</Descriptions.Item>
              )}
            </Descriptions>
          </Card>

          <Card title="Document" style={{ marginBottom: 16 }}>
            <Space>
              {report.fileUrl && <Button type="link" href={report.fileUrl} target="_blank">Download Word</Button>}
              {report.pdfUrl && <Button type="link" href={report.pdfUrl} target="_blank">Download PDF</Button>}
              {!report.fileUrl && !report.pdfUrl && <span style={{ color: '#999' }}>No document available yet</span>}
            </Space>
          </Card>

          <Card title="Version History" extra={<Button onClick={() => history.push(`/report/${report.id}/revisions`)}>View All Revisions</Button>}>
            <span>Current version: {report.versionNumber}</span>
          </Card>
        </>
      )}
    </PageContainer>
  );
};

export default ReportDetail;
