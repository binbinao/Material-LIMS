import { PageContainer } from '@ant-design/pro-components';
import {
  Card,
  Form,
  Input,
  Select,
  Button,
  Row,
  Col,
  Switch,
  message,
  Cascader,
  Space,
  Table,
  Typography,
} from 'antd';
import { useState, useEffect } from 'react';
import { useRequest } from 'ahooks';
import { history } from '@umijs/max';
import {
  getBrands,
  getRequestTypes,
  createRequest,
  searchParts,
  searchSuppliers,
  getAnalysisItemCascade,
} from '@/services/requestService';

const { TextArea } = Input;
const { Text } = Typography;

const RequestCreate: React.FC = () => {
  const [form] = Form.useForm();
  const [isProxy, setIsProxy] = useState(false);
  const [selectedItems, setSelectedItems] = useState<any[]>([]);
  const [cascadeData, setCascadeData] = useState<any[]>([]);
  const [partsOptions, setPartsOptions] = useState<any[]>([]);
  const [supplierOptions, setSupplierOptions] = useState<any[]>([]);
  const [partSearchLoading, setPartSearchLoading] = useState(false);
  const [supplierSearchLoading, setSupplierSearchLoading] = useState(false);

  // Load brands
  const { data: brandsData } = useRequest(getBrands, {
    defaultParams: [{ page: 0, size: 100 }],
  });
  const brands = brandsData?.data?.records ?? [];

  // Load request types
  const { data: requestTypesData, loading: requestTypesLoading } = useRequest(getRequestTypes, {
    defaultParams: [{ page: 1, size: 100 }],
  });
  const requestTypes = requestTypesData?.data?.records ?? [];

  // Load analysis item cascade
  useEffect(() => {
    getAnalysisItemCascade().then((res) => {
      if (res?.data) {
        setCascadeData(formatCascadeData(res.data));
      }
    });
  }, []);

  const formatCascadeData = (data: any[]) => {
    return data.map((group: any) => ({
      label: group.name,
      value: group.id,
      children: (group.types || []).map((type: any) => ({
        label: type.name,
        value: type.id,
        children: (type.items || []).map((item: any) => ({
          label: `${item.name} (${item.cost || '-'})`,
          value: item.id,
          item,
        })),
      })),
    }));
  };

  // Search parts
  const handlePartSearch = async (value: string) => {
    if (!value || value.length < 2) return;
    setPartSearchLoading(true);
    try {
      const res = await searchParts(value);
      setPartsOptions(
        (res?.data ?? []).map((p: any) => ({
          label: `${p.partNumber} - ${p.partName}`,
          value: p.partNumber,
          data: p,
        })),
      );
    } finally {
      setPartSearchLoading(false);
    }
  };

  // Handle part selection
  const handlePartSelect = (value: string, option: any) => {
    form.setFieldsValue({
      partNumber: option.data.partNumber,
      partName: option.data.partName,
      eco: option.data.eco,
    });
  };

  // Search suppliers
  const handleSupplierSearch = async (value: string) => {
    if (!value || value.length < 2) return;
    setSupplierSearchLoading(true);
    try {
      const res = await searchSuppliers(value);
      setSupplierOptions(
        (res?.data ?? []).map((s: any) => ({
          label: `${s.code} - ${s.name}`,
          value: s.code,
          data: s,
        })),
      );
    } finally {
      setSupplierSearchLoading(false);
    }
  };

  const handleSupplierSelect = (value: string, option: any) => {
    form.setFieldsValue({
      supplierCode: option.data.code,
      supplierName: option.data.name,
    });
  };

  // Handle analysis item selection
  const handleItemSelect = (values: string[][]) => {
    const items = values
      .filter((v) => v.length === 3)
      .map((v) => {
        const group = cascadeData.find((g) => g.value === v[0]);
        const type = group?.children?.find((t) => t.value === v[1]);
        const item = type?.children?.find((i) => i.value === v[2]);
        return item?.item;
      })
      .filter(Boolean);
    setSelectedItems(items);
  };

  // Submit
  const handleSubmit = async (values: any) => {
    const analysisItemIds = (values.analysisItems ?? [])
      .filter((v: string[]) => v.length === 3)
      .map((v: string[]) => v[2]);

    if (analysisItemIds.length === 0) {
      message.error('Please select at least one complete analysis item');
      return;
    }

    try {
      const data = {
        brandId: values.brandId,
        typeId: values.typeId,
        priority: values.priority,
        realRequesterName: values.realRequesterName,
        partNumber: values.partNumber,
        partName: values.partName,
        eco: values.eco,
        supplierCode: values.supplierCode,
        supplierName: values.supplierName,
        requestReason: values.requestReason,
        analysisItemIds,
        proxyRequest: isProxy,
      };
      const res = await createRequest(data);
      if (res?.code !== 200) {
        message.error(res?.message || 'Failed to create request');
        return;
      }
      message.success('Request created successfully');
      history.push('/request/list');
    } catch (err: any) {
      const errMsg =
        err?.response?.data?.message ||
        err?.data?.message ||
        err?.message ||
        'Failed to create request';
      message.error(errMsg);
    }
  };

  const totalCost = selectedItems.reduce((sum: number, item: any) => sum + (item?.cost || 0), 0);

  return (
    <PageContainer title="Create Request">
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Card title="Basic Information" style={{ marginBottom: 16 }}>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="brandId" label="Brand" rules={[{ required: true }]}>
                <Select
                  showSearch
                  placeholder="Select brand"
                  optionFilterProp="label"
                  options={brands.map((b: any) => ({ label: b.name, value: b.id }))}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="typeId" label="Request Type" rules={[{ required: true }]}>
                <Select
                  showSearch
                  loading={requestTypesLoading}
                  placeholder="Select request type"
                  optionFilterProp="label"
                  options={requestTypes.map((t: any) => ({
                    label: t.taskDurationDays != null ? `${t.name} (${t.taskDurationDays}d)` : t.name,
                    value: t.id,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="priority" label="Priority" initialValue="NORMAL">
                <Select
                  options={[
                    { label: 'Low', value: 'LOW' },
                    { label: 'Normal', value: 'NORMAL' },
                    { label: 'High', value: 'HIGH' },
                    { label: 'Urgent', value: 'URGENT' },
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={16}>
            <Col span={8}>
              <Form.Item label="Proxy Request">
                <Switch checked={isProxy} onChange={setIsProxy} />
              </Form.Item>
            </Col>
            {isProxy && (
              <Col span={8}>
                <Form.Item name="realRequesterName" label="Real Requester Name" rules={[{ required: isProxy }]}>
                  <Input placeholder="Name of the actual requester" />
                </Form.Item>
              </Col>
            )}
          </Row>
        </Card>

        <Card title="Part & Supplier" style={{ marginBottom: 16 }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="partSearch" label="Search Part Number">
                <Select
                  showSearch
                  placeholder="Type to search parts..."
                  filterOption={false}
                  onSearch={handlePartSearch}
                  onSelect={handlePartSelect}
                  loading={partSearchLoading}
                  options={partsOptions}
                  notFoundContent={partSearchLoading ? 'Searching...' : 'Type keyword to search'}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="supplierSearch" label="Search Supplier">
                <Select
                  showSearch
                  placeholder="Type to search suppliers..."
                  filterOption={false}
                  onSearch={handleSupplierSearch}
                  onSelect={handleSupplierSelect}
                  loading={supplierSearchLoading}
                  options={supplierOptions}
                  notFoundContent={supplierSearchLoading ? 'Searching...' : 'Type keyword to search'}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={6}>
              <Form.Item name="partNumber" label="Part Number">
                <Input placeholder="Auto-filled or manual" />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="partName" label="Part Name">
                <Input placeholder="Auto-filled or manual" />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="eco" label="ECO">
                <Input />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="supplierName" label="Supplier Name">
                <Input placeholder="Auto-filled or manual" />
              </Form.Item>
            </Col>
          </Row>
        </Card>

        <Card title="Request Details" style={{ marginBottom: 16 }}>
          <Form.Item name="requestReason" label="Request Reason" rules={[{ required: true }]}>
            <TextArea rows={4} placeholder="Describe the reason for this request in detail" />
          </Form.Item>
        </Card>

        <Card title="Analysis Items" style={{ marginBottom: 16 }}>
          <Form.Item name="analysisItems" label="Select Analysis Items" rules={[{ required: true }]}>
            <Cascader
              style={{ width: '100%' }}
              options={cascadeData}
              multiple
              placeholder="Test Group → Analysis Type → Analysis Item"
              onChange={handleItemSelect}
            />
          </Form.Item>

          {selectedItems.length > 0 && (
            <Table
              dataSource={selectedItems}
              rowKey="id"
              size="small"
              pagination={false}
              columns={[
                { title: 'Item', dataIndex: 'name' },
                { title: 'Standards', dataIndex: 'testStandards' },
                { title: 'Cost', dataIndex: 'cost', render: (v) => v ?? '-' },
                { title: 'Unit Price', dataIndex: 'unitPrice', render: (v) => v ?? '-' },
                { title: 'Unit', dataIndex: 'unit', render: (v) => v ?? '-' },
              ]}
              summary={() => (
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0} colSpan={3}>
                    <Text strong>Total Cost</Text>
                  </Table.Summary.Cell>
                  <Table.Summary.Cell index={1} colSpan={2}>
                    <Text strong>{totalCost.toFixed(2)}</Text>
                  </Table.Summary.Cell>
                </Table.Summary.Row>
              )}
            />
          )}
        </Card>

        <Space>
          <Button type="primary" htmlType="submit" size="large">
            Submit Request
          </Button>
          <Button onClick={() => history.back()}>Cancel</Button>
        </Space>
      </Form>
    </PageContainer>
  );
};

export default RequestCreate;
