import { PageContainer } from '@ant-design/pro-components';
import { Card, Descriptions, Tag, Button, Steps, Table, Modal, Form, Input, App, Divider, Space, Timeline, Select } from 'antd';
import { useParams, history, useAccess, useIntl } from '@umijs/max';
import { useRequest } from 'ahooks';
import { useState } from 'react';
import { getRequest, getRequestTasks, getRequestWorkflow, submitRequest, assignRequest, rejectRequest, receiveSample, startReporting, completeRequest, updateAnalysisTask, getAdminUsers, createReport } from '@/services/requestService';
import dayjs from 'dayjs';

const statusSteps = ['DRAFT', 'SUBMITTED', 'ASSIGNED', 'SAMPLING', 'REPORTING', 'APPROVING', 'COMPLETED'];

const RequestDetail: React.FC = () => {
  const params = useParams<{ id: string }>();
  const { message, modal } = App.useApp();
  const intl = useIntl();

  const { data: requestData, loading: reqLoading, refresh } = useRequest(() => getRequest(params.id));
  const { data: tasksData, refresh: refreshTasks } = useRequest(() => getRequestTasks(params.id));
  const { data: workflowData } = useRequest(() => getRequestWorkflow(params.id));
  const { data: usersData } = useRequest(() => getAdminUsers({ page: 1, size: 200 }));

  const req = requestData?.data;
  const tasks = tasksData?.data ?? [];
  const workflow = workflowData?.data;

  const getStatusText = (s: string) => intl.formatMessage({ id: `request.status.${s}`, defaultMessage: s });

  const statusColorMap: Record<string, string> = {
    DRAFT: 'default', SUBMITTED: 'processing', ASSIGNED: 'processing',
    SAMPLING: 'warning', REPORTING: 'warning', APPROVING: 'orange',
    COMPLETED: 'success', REJECTED: 'error',
  };

  const getPriorityText = (p: string) => intl.formatMessage({ id: `request.priority.${p}`, defaultMessage: p });
  const priorityColorMap: Record<string, string> = { LOW: 'default', NORMAL: 'blue', HIGH: 'orange', URGENT: 'red' };

  const getTaskStatusText = (s: string) => intl.formatMessage({ id: `request.task.${s}`, defaultMessage: s });
  const taskColorMap: Record<string, string> = {
    PENDING: 'default', IN_PROGRESS: 'processing', COMPLETED: 'success', DELAYED: 'error',
  };

  const engineers = (usersData?.data?.records ?? []).filter((u: any) =>
    (u.roles || '').split(',').includes('ENGINEER'),
  );

  const [assignOpen, setAssignOpen] = useState(false);
  const [assignMap, setAssignMap] = useState<Record<string, string>>({});
  const [creatingReport, setCreatingReport] = useState(false);

  // Issue #11: Form instances for the reject-modal and the
  // receive-sample-modal. Reading values via document.getElementById
  // bypassed Ant's <Form rules={[{ required: true }]}> validation;
  // useForm + validateFields() makes those rules actually fire.
  const [rejectForm] = Form.useForm();
  const [sampleForm] = Form.useForm();

  // Issue #24: gate action buttons by role.
  const access = useAccess();

  const currentStepIndex = req ? statusSteps.indexOf(req.status) : -1;

  const handleAssign = async () => {
    const assignments = tasks
      .map((t: API.AnalysisTask) => ({ taskId: t.id, engineerId: assignMap[t.id] }))
      .filter((a: { engineerId?: string }) => !!a.engineerId);
    if (assignments.length === 0) {
      message.error(intl.formatMessage({ id: 'request.detail.msg.selectEngineer', defaultMessage: 'Please assign an engineer to at least one task' }));
      return;
    }
    try {
      const res = await assignRequest(params.id, assignments as { taskId: string; engineerId: string }[]);
      if (res?.code !== 200) {
        message.error(res?.message || intl.formatMessage({ id: 'request.detail.msg.assignmentFailed', defaultMessage: 'Assignment failed' }));
        return;
      }
      message.success(intl.formatMessage({ id: 'request.detail.msg.requestAssigned', defaultMessage: 'Request assigned' }));
      setAssignOpen(false);
      setAssignMap({});
      refresh();
      refreshTasks();
    } catch {
      message.error(intl.formatMessage({ id: 'request.detail.msg.assignmentFailed', defaultMessage: 'Assignment failed' }));
    }
  };

  const handleAction = async (action: string) => {
    try {
      switch (action) {
        case 'submit':
          await submitRequest(params.id);
          message.success(intl.formatMessage({ id: 'request.detail.msg.requestSubmitted', defaultMessage: 'Request submitted' }));
          refresh();
          break;
        case 'reject':
          modal.confirm({
            title: intl.formatMessage({ id: 'request.detail.modal.reject.title', defaultMessage: 'Reject Request' }),
            content: (
              <Form form={rejectForm} layout="vertical">
                <Form.Item name="reason" label={intl.formatMessage({ id: 'request.detail.modal.reject.reason', defaultMessage: 'Reason' })} rules={[{ required: true }]}>
                  <Input.TextArea rows={3} placeholder={intl.formatMessage({ id: 'request.detail.modal.reject.placeholder', defaultMessage: 'Why is this request being rejected?' })} />
                </Form.Item>
              </Form>
            ),
            onOk: async () => {
              try {
                const values = await rejectForm.validateFields();
                await rejectRequest(params.id, { reason: values.reason });
                rejectForm.resetFields();
                message.success(intl.formatMessage({ id: 'request.detail.msg.requestRejected', defaultMessage: 'Request rejected' }));
                refresh();
              } catch {
                throw new Error('validation');
              }
            },
            onCancel: () => rejectForm.resetFields(),
          });
          return;
        case 'receive-sample':
          modal.confirm({
            title: intl.formatMessage({ id: 'request.detail.modal.receiveSample.title', defaultMessage: 'Receive Sample' }),
            content: (
              <Form form={sampleForm} layout="vertical">
                <Form.Item name="deliveryNote" label={intl.formatMessage({ id: 'request.detail.modal.receiveSample.deliveryNote', defaultMessage: 'Delivery Note' })}>
                  <Input.TextArea rows={2} />
                </Form.Item>
              </Form>
            ),
            onOk: async () => {
              try {
                const values = await sampleForm.validateFields();
                await receiveSample(params.id, { deliveryNote: values.deliveryNote || '' });
                sampleForm.resetFields();
                message.success(intl.formatMessage({ id: 'request.detail.msg.sampleReceived', defaultMessage: 'Sample received' }));
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
          message.success(intl.formatMessage({ id: 'request.detail.msg.reportingStarted', defaultMessage: 'Reporting phase started' }));
          refresh();
          break;
        case 'complete':
          await completeRequest(params.id);
          message.success(intl.formatMessage({ id: 'request.detail.msg.requestCompleted', defaultMessage: 'Request completed' }));
          refresh();
          break;
      }
    } catch {
      message.error(intl.formatMessage({ id: 'request.detail.msg.actionFailed', defaultMessage: 'Action failed' }));
    }
  };

  const handleTaskStatusChange = async (taskId: string, status: string) => {
    try {
      await updateAnalysisTask(taskId, { status });
      message.success(intl.formatMessage({ id: 'request.detail.msg.taskUpdated', defaultMessage: 'Task updated' }));
      refresh();
    } catch {
      message.error(intl.formatMessage({ id: 'request.detail.msg.taskUpdateFailed', defaultMessage: 'Failed to update task' }));
    }
  };

  const actionButtons = () => {
    if (!req) return null;
    const btns: React.ReactNode[] = [];
    switch (req.status) {
      case 'DRAFT':
        if (access.canManager) {
          btns.push(<Button key="submit" type="primary" onClick={() => handleAction('submit')}>{intl.formatMessage({ id: 'request.detail.btn.submit', defaultMessage: 'Submit' })}</Button>);
        }
        break;
      case 'SUBMITTED':
        if (access.canManager) {
          btns.push(<Button key="assign" type="primary" onClick={() => setAssignOpen(true)}>{intl.formatMessage({ id: 'request.detail.btn.assign', defaultMessage: 'Assign' })}</Button>);
          btns.push(<Button key="reject" danger onClick={() => handleAction('reject')}>{intl.formatMessage({ id: 'request.detail.btn.reject', defaultMessage: 'Reject' })}</Button>);
        }
        break;
      case 'ASSIGNED':
        if (access.canTechnician || access.canManager) {
          btns.push(<Button key="receive" type="primary" onClick={() => handleAction('receive-sample')}>{intl.formatMessage({ id: 'request.detail.btn.receiveSample', defaultMessage: 'Receive Sample' })}</Button>);
        }
        if (access.canManager) {
          btns.push(<Button key="reject" danger onClick={() => handleAction('reject')}>{intl.formatMessage({ id: 'request.detail.btn.reject', defaultMessage: 'Reject' })}</Button>);
        }
        break;
      case 'SAMPLING':
        if (access.canEngineer || access.canManager) {
          btns.push(<Button key="report" type="primary" onClick={() => handleAction('start-reporting')}>{intl.formatMessage({ id: 'request.detail.btn.startReporting', defaultMessage: 'Start Reporting' })}</Button>);
        }
        break;
      case 'APPROVING':
        if (access.canManager) {
          btns.push(<Button key="complete" type="primary" onClick={() => handleAction('complete')}>{intl.formatMessage({ id: 'request.detail.btn.complete', defaultMessage: 'Complete' })}</Button>);
        }
        break;
    }
    return btns.length > 0 ? <Space>{btns}</Space> : null;
  };

  const taskColumns = [
    { title: intl.formatMessage({ id: 'request.detail.table.itemId', defaultMessage: 'Item ID' }), dataIndex: 'itemId', width: 120 },
    { title: intl.formatMessage({ id: 'request.detail.table.assignee', defaultMessage: 'Assignee' }), dataIndex: 'assigneeId', width: 120, render: (v: string) => v || '-' },
    {
      title: intl.formatMessage({ id: 'request.detail.table.status', defaultMessage: 'Status' }), dataIndex: 'status', width: 120,
      render: (v: string) => <Tag color={taskColorMap[v] || 'default'}>{getTaskStatusText(v)}</Tag>,
    },
    { title: intl.formatMessage({ id: 'request.detail.table.delayReason', defaultMessage: 'Delay Reason' }), dataIndex: 'delayReason', ellipsis: true },
    {
      title: intl.formatMessage({ id: 'request.detail.table.started', defaultMessage: 'Started' }), dataIndex: 'startedAt', width: 160,
      render: (v: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: intl.formatMessage({ id: 'request.detail.table.completed', defaultMessage: 'Completed' }), dataIndex: 'completedAt', width: 160,
      render: (v: string) => v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-',
    },
    {
      title: intl.formatMessage({ id: 'request.detail.table.action', defaultMessage: 'Action' }), width: 180,
      render: (_: any, record: API.AnalysisTask) => {
        if (record.status === 'PENDING') return <Button size="small" onClick={() => handleTaskStatusChange(record.id, 'IN_PROGRESS')}>{intl.formatMessage({ id: 'request.detail.btn.start', defaultMessage: 'Start' })}</Button>;
        if (record.status === 'IN_PROGRESS') return <Button size="small" type="primary" onClick={() => handleTaskStatusChange(record.id, 'COMPLETED')}>{intl.formatMessage({ id: 'request.detail.btn.complete', defaultMessage: 'Complete' })}</Button>;
        return null;
      },
    },
  ];

  return (
    <PageContainer
      title={req?.requestNo || intl.formatMessage({ id: 'menu.request.list' })}
      onBack={() => history.push('/request/list')}
      extra={actionButtons()}
    >
      {req && (
        <>
          <Card style={{ marginBottom: 16 }}>
            <Steps
              current={currentStepIndex >= 0 ? currentStepIndex : 0}
              items={statusSteps.map((s) => ({
                title: getStatusText(s),
                status: req.status === 'REJECTED' && s === statusSteps[currentStepIndex] ? 'error' : undefined,
              }))}
            />
          </Card>

          <Card title={intl.formatMessage({ id: 'request.detail.requestInfo' })} style={{ marginBottom: 16 }}>
            <Descriptions column={3}>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.requestNo', defaultMessage: 'Request No' })}>{req.requestNo}</Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.brand', defaultMessage: 'Brand' })}>{req.brandId}</Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.priority', defaultMessage: 'Priority' })}>
                <Tag color={priorityColorMap[req.priority]}>{getPriorityText(req.priority)}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.status', defaultMessage: 'Status' })}>
                <Tag color={statusColorMap[req.status]}>{getStatusText(req.status)}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.dueDate', defaultMessage: 'Due Date' })}>
                {req.dueDate ? dayjs(req.dueDate).format('YYYY-MM-DD') : '-'}
              </Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.totalCost', defaultMessage: 'Total Cost' })}>
                {req.totalCost != null ? `¥${req.totalCost.toFixed(2)}` : '-'}
              </Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.partNumber', defaultMessage: 'Part Number' })}>{req.partNumber || '-'}</Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.partName', defaultMessage: 'Part Name' })}>{req.partName || '-'}</Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.eco', defaultMessage: 'ECO' })}>{req.eco || '-'}</Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.supplier', defaultMessage: 'Supplier' })}>{req.supplierName || '-'}</Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.supplierCode', defaultMessage: 'Supplier Code' })}>{req.supplierCode || '-'}</Descriptions.Item>
              <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.requestReason', defaultMessage: 'Request Reason' })} span={3}>{req.requestReason || '-'}</Descriptions.Item>
              {req.sampleDeliveryNote && (
                <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.deliveryNote', defaultMessage: 'Delivery Note' })} span={3}>{req.sampleDeliveryNote}</Descriptions.Item>
              )}
            </Descriptions>
          </Card>

          <Card title={intl.formatMessage({ id: 'request.detail.analysisTasks' })} style={{ marginBottom: 16 }}>
            <Table
              columns={taskColumns}
              dataSource={tasks}
              rowKey="id"
              size="small"
              pagination={false}
            />
          </Card>

          {workflow && (
            <Card title={intl.formatMessage({ id: 'request.detail.workflowStatus' })}>
              <Descriptions column={2}>
                <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.currentTask', defaultMessage: 'Current Task' })}>{workflow.taskName || '-'}</Descriptions.Item>
                <Descriptions.Item label={intl.formatMessage({ id: 'request.detail.label.assignee', defaultMessage: 'Assignee' })}>{workflow.assignee || '-'}</Descriptions.Item>
              </Descriptions>
            </Card>
          )}

          {/* Issue #56 (P2): expose createReport() so the Report workflow
              is reachable from the UI. The button appears once the request
              has tasks/results worth reporting (REPORTING, APPROVING, or
              COMPLETED). Gated on ENGINEER/MANAGER/ADMIN per backend
              @PreAuthorize. */}
          {(req?.status === 'REPORTING' || req?.status === 'APPROVING' || req?.status === 'COMPLETED')
            && (access.canEngineer || access.canManager) && (
            <Card title={intl.formatMessage({ id: 'request.detail.report' })}>
              <Button
                key="generate-report"
                type="primary"
                loading={creatingReport}
                onClick={async () => {
                  setCreatingReport(true);
                  try {
                    const res = await createReport(params.id);
                    if (res?.code === 200 && res?.data?.id) {
                      message.success(intl.formatMessage({ id: 'request.detail.msg.reportGenerated', defaultMessage: 'Report generated' }));
                      history.push(`/report/${res.data.id}`);
                    } else {
                      message.success(intl.formatMessage({ id: 'request.detail.msg.reportGenerated', defaultMessage: 'Report generated' }));
                      refresh();
                    }
                  } catch {
                    message.error(intl.formatMessage({ id: 'request.detail.msg.reportFailed', defaultMessage: 'Failed to generate report' }));
                  } finally {
                    setCreatingReport(false);
                  }
                }}
              >
                {intl.formatMessage({ id: 'request.detail.btn.generateReport', defaultMessage: 'Generate Report' })}
              </Button>
            </Card>
          )}

          <Modal
            title={intl.formatMessage({ id: 'request.detail.assignEngineers' })}
            open={assignOpen}
            onOk={handleAssign}
            onCancel={() => setAssignOpen(false)}
            okText={intl.formatMessage({ id: 'request.detail.btn.assign', defaultMessage: 'Assign' })}
            width={640}
          >
            <Table
              dataSource={tasks}
              rowKey="id"
              size="small"
              pagination={false}
              columns={[
                { title: intl.formatMessage({ id: 'request.detail.table.itemId', defaultMessage: 'Item ID' }), dataIndex: 'itemId', width: 160 },
                {
                  title: intl.formatMessage({ id: 'request.detail.table.engineer', defaultMessage: 'Engineer' }),
                  render: (_: any, record: API.AnalysisTask) => (
                    <Select
                      style={{ width: '100%' }}
                      placeholder={intl.formatMessage({ id: 'request.detail.btn.assign', defaultMessage: 'Select engineer' })}
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
