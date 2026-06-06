import { PageContainer } from '@ant-design/pro-components';
import { Card, Descriptions, Tag, Button, Steps, Table, Modal, Form, Input, App, Divider, Space, Timeline } from 'antd';
import { useParams, history } from '@umijs/max';
import { useRequest } from 'ahooks';
import { getRequest, getRequestTasks, getRequestWorkflow, submitRequest, rejectRequest, receiveSample, startReporting, completeRequest, updateAnalysisTask } from '@/services/requestService';
import dayjs from 'dayjs';

const statusMap: Record<string, { color: string; text: string }> = {
  DRAFT: { color: 'default', text: 'Draft' },
  SUBMITTED: { color: 'processing', text: 'Submitted' },
  ASSIGNED: { color: 'processing', text: 'Assigned' },
  SAMPLING: { color: 'warning', text: 'Sampling' },
  REPORTING: { color: 'warning', text: 'Reporting' },
  APPROVING: { color: 'orange', text: 'Approving' },
  COMPLETED: { color: 'success', text: 'Completed' },
  REJECTED: { color: 'error', text: 'Rejected' },
};

const priorityMap: Record<string, { color: string; text: string }> = {
  LOW: { color: 'default', text: 'Low' },
  NORMAL: { color: 'blue', text: 'Normal' },
  HIGH: { color: 'orange', text: 'High' },
  URGENT: { color: 'red', text: 'Urgent' },
};

const statusSteps = ['DRAFT', 'SUBMITTED', 'ASSIGNED', 'SAMPLING', 'REPORTING', 'APPROVING', 'COMPLETED'];

const taskStatusMap: Record<string, { color: string }> = {
  PENDING: { color: 'default' },
  IN_PROGRESS: { color: 'processing' },
  COMPLETED: { color: 'success' },
  DELAYED: { color: 'error' },
};

const RequestDetail: React.FC = () => {
  const params = useParams<{ id: string }>();
  const { message, modal } = App.useApp();

  const { data: requestData, loading: reqLoading, refresh } = useRequest(() => getRequest(params.id));
  const { data: tasksData } = useRequest(() => getRequestTasks(params.id));
  const { data: workflowData } = useRequest(() => getRequestWorkflow(params.id));

  const req = requestData?.data;
  const tasks = tasksData?.data ?? [];
  const workflow = workflowData?.data;

  const currentStepIndex = req ? statusSteps.indexOf(req.status) : -1;

  const handleAction = async (action: string) => {
    try {
      switch (action) {
        case 'submit':
          await submitRequest(params.id);
          message.success('Request submitted');
          break;
        case 'reject':
          modal.confirm({
            title: 'Reject Request',
            content: (
              <Form id="reject-form">
                <Form.Item name="reason" label="Reason" rules={[{ required: true }]}>
                  <Input.TextArea rows={3} id="reject-reason" />
                </Form.Item>
              </Form>
            ),
            onOk: async () => {
              const el = document.getElementById('reject-reason') as HTMLTextAreaElement;
              await rejectRequest(params.id, { reason: el?.value || '' });
              message.success('Request rejected');
              refresh();
            },
          });
          return;
        case 'receive-sample':
          modal.confirm({
            title: 'Receive Sample',
            content: (
              <Form id="sample-form">
                <Form.Item name="deliveryNote" label="Delivery Note">
                  <Input.TextArea rows={2} id="delivery-note" />
                </Form.Item>
              </Form>
            ),
            onOk: async () => {
              const el = document.getElementById('delivery-note') as HTMLTextAreaElement;
              await receiveSample(params.id, { deliveryNote: el?.value || '' });
              message.success('Sample received');
              refresh();
            },
          });
          return;
        case 'start-reporting':
          await startReporting(params.id);
          message.success('Reporting phase started');
          break;
        case 'complete':
          await completeRequest(params.id);
          message.success('Request completed');
          break;
      }
      refresh();
    } catch {
      message.error('Action failed');
    }
  };

  const handleTaskStatusChange = async (taskId: string, status: string) => {
    try {
      await updateAnalysisTask(taskId, { status });
      message.success('Task updated');
      refresh();
    } catch {
      message.error('Failed to update task');
    }
  };

  const actionButtons = () => {
    if (!req) return null;
    const btns: React.ReactNode[] = [];
    switch (req.status) {
      case 'DRAFT':
        btns.push(<Button key="submit" type="primary" onClick={() => handleAction('submit')}>Submit</Button>);
        break;
      case 'ASSIGNED':
        btns.push(<Button key="receive" type="primary" onClick={() => handleAction('receive-sample')}>Receive Sample</Button>);
        btns.push(<Button key="reject" danger onClick={() => handleAction('reject')}>Reject</Button>);
        break;
      case 'SAMPLING':
        btns.push(<Button key="report" type="primary" onClick={() => handleAction('start-reporting')}>Start Reporting</Button>);
        break;
      case 'APPROVING':
        btns.push(<Button key="complete" type="primary" onClick={() => handleAction('complete')}>Complete</Button>);
        break;
    }
    return btns.length > 0 ? <Space>{btns}</Space> : null;
  };

  const taskColumns = [
    { title: 'Item ID', dataIndex: 'itemId', width: 120 },
    { title: 'Assignee', dataIndex: 'assigneeId', width: 120, render: (v: string) => v || '-' },
    {
      title: 'Status', dataIndex: 'status', width: 120,
      render: (v: string) => <Tag color={taskStatusMap[v]?.color || 'default'}>{v}</Tag>,
    },
    { title: 'Delay Reason', dataIndex: 'delayReason', ellipsis: true },
    {
      title: 'Started', dataIndex: 'startedAt', width: 160,
      render: (v: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: 'Completed', dataIndex: 'completedAt', width: 160,
      render: (v: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: 'Action', width: 180,
      render: (_: any, record: API.AnalysisTask) => {
        if (record.status === 'PENDING') return <Button size="small" onClick={() => handleTaskStatusChange(record.id, 'IN_PROGRESS')}>Start</Button>;
        if (record.status === 'IN_PROGRESS') return <Button size="small" type="primary" onClick={() => handleTaskStatusChange(record.id, 'COMPLETED')}>Complete</Button>;
        return null;
      },
    },
  ];

  return (
    <PageContainer
      title={req?.requestNo || 'Request Detail'}
      onBack={() => history.push('/request/list')}
      extra={actionButtons()}
    >
      {req && (
        <>
          <Card style={{ marginBottom: 16 }}>
            <Steps
              current={currentStepIndex >= 0 ? currentStepIndex : 0}
              items={statusSteps.map((s) => ({
                title: statusMap[s]?.text || s,
                status: req.status === 'REJECTED' && s === statusSteps[currentStepIndex] ? 'error' : undefined,
              }))}
            />
          </Card>

          <Card title="Request Information" style={{ marginBottom: 16 }}>
            <Descriptions column={3}>
              <Descriptions.Item label="Request No">{req.requestNo}</Descriptions.Item>
              <Descriptions.Item label="Brand">{req.brandId}</Descriptions.Item>
              <Descriptions.Item label="Priority">
                <Tag color={priorityMap[req.priority]?.color}>{priorityMap[req.priority]?.text || req.priority}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={statusMap[req.status]?.color}>{statusMap[req.status]?.text || req.status}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Due Date">
                {req.dueDate ? dayjs(req.dueDate).format('YYYY-MM-DD') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Total Cost">
                {req.totalCost != null ? `¥${req.totalCost.toFixed(2)}` : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Part Number">{req.partNumber || '-'}</Descriptions.Item>
              <Descriptions.Item label="Part Name">{req.partName || '-'}</Descriptions.Item>
              <Descriptions.Item label="ECO">{req.eco || '-'}</Descriptions.Item>
              <Descriptions.Item label="Supplier">{req.supplierName || '-'}</Descriptions.Item>
              <Descriptions.Item label="Supplier Code">{req.supplierCode || '-'}</Descriptions.Item>
              <Descriptions.Item label="Request Reason" span={3}>{req.requestReason || '-'}</Descriptions.Item>
              {req.sampleDeliveryNote && (
                <Descriptions.Item label="Delivery Note" span={3}>{req.sampleDeliveryNote}</Descriptions.Item>
              )}
            </Descriptions>
          </Card>

          <Card title="Analysis Tasks" style={{ marginBottom: 16 }}>
            <Table
              columns={taskColumns}
              dataSource={tasks}
              rowKey="id"
              size="small"
              pagination={false}
            />
          </Card>

          {workflow && (
            <Card title="Workflow Status">
              <Descriptions column={2}>
                <Descriptions.Item label="Current Task">{workflow.taskName || '-'}</Descriptions.Item>
                <Descriptions.Item label="Assignee">{workflow.assignee || '-'}</Descriptions.Item>
              </Descriptions>
            </Card>
          )}
        </>
      )}
    </PageContainer>
  );
};

export default RequestDetail;
