import { PageContainer } from '@ant-design/pro-components';
import { Card, Descriptions, Tag, Button, Steps, Table, Modal, Form, Input, App, Divider, Space, Timeline, Select } from 'antd';
import { useParams, history } from '@umijs/max';
import { useRequest } from 'ahooks';
import { useState } from 'react';
import { getRequest, getRequestTasks, getRequestWorkflow, submitRequest, assignRequest, rejectRequest, receiveSample, startReporting, completeRequest, updateAnalysisTask, getAdminUsers } from '@/services/requestService';
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
  const { data: tasksData, refresh: refreshTasks } = useRequest(() => getRequestTasks(params.id));
  const { data: workflowData } = useRequest(() => getRequestWorkflow(params.id));
  const { data: usersData } = useRequest(() => getAdminUsers({ page: 1, size: 200 }));

  const req = requestData?.data;
  const tasks = tasksData?.data ?? [];
  const workflow = workflowData?.data;

  const engineers = (usersData?.data?.records ?? []).filter((u: any) =>
    (u.roles || '').split(',').includes('ENGINEER'),
  );

  const [assignOpen, setAssignOpen] = useState(false);
  const [assignMap, setAssignMap] = useState<Record<string, string>>({});

  // Issue #11: Form instances for the reject-modal and the
  // receive-sample-modal. Reading values via document.getElementById
  // bypassed Ant's <Form rules={[{ required: true }]}> validation;
  // useForm + validateFields() makes those rules actually fire.
  const [rejectForm] = Form.useForm();
  const [sampleForm] = Form.useForm();

  const currentStepIndex = req ? statusSteps.indexOf(req.status) : -1;

  const handleAssign = async () => {
    const assignments = tasks
      .map((t: API.AnalysisTask) => ({ taskId: t.id, engineerId: assignMap[t.id] }))
      .filter((a: { engineerId?: string }) => !!a.engineerId);
    if (assignments.length === 0) {
      message.error('Please assign an engineer to at least one task');
      return;
    }
    try {
      const res = await assignRequest(params.id, assignments as { taskId: string; engineerId: string }[]);
      if (res?.code !== 200) {
        message.error(res?.message || 'Assignment failed');
        return;
      }
      message.success('Request assigned');
      setAssignOpen(false);
      setAssignMap({});
      refresh();
      refreshTasks();
    } catch {
      message.error('Assignment failed');
    }
  };

  const handleAction = async (action: string) => {
    try {
      switch (action) {
        case 'submit':
          await submitRequest(params.id);
          message.success('Request submitted');
          refresh();
          break;
        case 'reject':
          modal.confirm({
            title: 'Reject Request',
            content: (
              <Form form={rejectForm} layout="vertical">
                <Form.Item name="reason" label="Reason" rules={[{ required: true, message: 'Please enter a reject reason' }]}>
                  <Input.TextArea rows={3} placeholder="Why is this request being rejected?" />
                </Form.Item>
              </Form>
            ),
            onOk: async () => {
              try {
                const values = await rejectForm.validateFields();
                await rejectRequest(params.id, { reason: values.reason });
                rejectForm.resetFields();
                message.success('Request rejected');
                refresh();
              } catch {
                // validateFields rejected — Ant already shows the error
                // inline; re-throw so the modal stays open.
                throw new Error('validation');
              }
            },
            onCancel: () => rejectForm.resetFields(),
          });
          return;
        case 'receive-sample':
          modal.confirm({
            title: 'Receive Sample',
            content: (
              <Form form={sampleForm} layout="vertical">
                <Form.Item name="deliveryNote" label="Delivery Note">
                  <Input.TextArea rows={2} placeholder="Optional delivery note" />
                </Form.Item>
              </Form>
            ),
            onOk: async () => {
              try {
                const values = await sampleForm.validateFields();
                await receiveSample(params.id, { deliveryNote: values.deliveryNote || '' });
                sampleForm.resetFields();
                message.success('Sample received');
                refresh();
              } catch {
                throw new Error('validation');
              }
            },
            onCancel: () => sampleForm.resetFields(),
          });
          return;
        case 'start-reporting':
          await startReporting(params.id);
          message.success('Reporting phase started');
          refresh();
          break;
        case 'complete':
          await completeRequest(params.id);
          message.success('Request completed');
          refresh();
          break;
      }
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
      case 'SUBMITTED':
        btns.push(<Button key="assign" type="primary" onClick={() => setAssignOpen(true)}>Assign</Button>);
        btns.push(<Button key="reject" danger onClick={() => handleAction('reject')}>Reject</Button>);
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

          <Modal
            title="Assign Engineers"
            open={assignOpen}
            onOk={handleAssign}
            onCancel={() => setAssignOpen(false)}
            okText="Assign"
            width={640}
          >
            <Table
              dataSource={tasks}
              rowKey="id"
              size="small"
              pagination={false}
              columns={[
                { title: 'Item ID', dataIndex: 'itemId', width: 160 },
                {
                  title: 'Engineer',
                  render: (_: any, record: API.AnalysisTask) => (
                    <Select
                      style={{ width: '100%' }}
                      placeholder="Select engineer"
                      value={assignMap[record.id]}
                      onChange={(v) => setAssignMap((m) => ({ ...m, [record.id]: v }))}
                      options={engineers.map((e: any) => ({
                        label: e.displayName || e.email || e.id,
                        value: e.id,
                      }))}
                    />
                  ),
                },
              ]}
            />
          </Modal>
        </>
      )}
    </PageContainer>
  );
};

export default RequestDetail;
