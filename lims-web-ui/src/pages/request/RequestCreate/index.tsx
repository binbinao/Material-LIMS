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
import { history, useIntl } from '@umijs/max';
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
  const intl = useIntl();
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
      message.error(intl.formatMessage({ id: 'request.create.error.noItem' }));
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
        message.error(res?.message || intl.formatMessage({ id: 'request.create.error.failed' }));
        return;
      }
      message.success(intl.formatMessage({ id: 'request.create.success' }));
      history.push('/request/list');
    } catch (err: any) {
      const errMsg =
        err?.response?.data?.message ||
        err?.data?.message ||
        err?.message ||
        intl.formatMessage({ id: 'request.create.error.failed' });
      message.error(errMsg);
    }
  };

  const totalCost = selectedItems.reduce((sum: number, item: any) => sum + (item?.cost || 0), 0);

  return (
    <PageContainer title={intl.formatMessage({ id: 'menu.request.create' })}>
      <Form form={form} layout="vertical" onFinish={handleSubmit}>
        <Card title={intl.formatMessage({ id: 'request.create.basicInfo' })} style={{ marginBottom: 16 }}>
          <Row gutter={16}>
            <Col span={8}>
              <Form.Item name="brandId" label={intl.formatMessage({ id: 'request.create.form.brand' })} rules={[{ required: true }]}>
                <Select
                  showSearch
                  placeholder={intl.formatMessage({ id: 'common.search' })}
                  optionFilterProp="label"
                  options={brands.map((b: any) => ({ label: b.name, value: b.id }))}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="typeId" label={intl.formatMessage({ id: 'request.create.form.requestType' })} rules={[{ required: true }]}>
                <Select
                  showSearch
                  loading={requestTypesLoading}
                  placeholder={intl.formatMessage({ id: 'common.search' })}
                  optionFilterProp="label"
                  options={requestTypes.map((t: any) => ({
                    label: t.taskDurationDays != null ? `${t.name} (${t.taskDurationDays}d)` : t.name,
                    value: t.id,
                  }))}
                />
              </Form.Item>
            </Col>
            <Col span={8}>
              <Form.Item name="priority" label={intl.formatMessage({ id: 'request.create.form.priority' })} initialValue="NORMAL">
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
              <Form.Item label={intl.formatMessage({ id: 'request.create.form.proxyRequest' })}>
                <Switch checked={isProxy} onChange={setIsProxy} />
              </Form.Item>
            </Col>
            {isProxy && (
              <Col span={8}>
                <Form.Item name="realRequesterName" label={intl.formatMessage({ id: 'request.create.form.realRequesterName' })} rules={[{ required: isProxy }]}>
                  <Input placeholder={intl.formatMessage({ id: 'request.create.form.realRequesterName' })} />
                </Form.Item>
              </Col>
            )}
          </Row>
        </Card>

        <Card title={intl.formatMessage({ id: 'request.create.partSupplier' })} style={{ marginBottom: 16 }}>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item name="partSearch" label={intl.formatMessage({ id: 'request.create.form.searchPart' })}>
                <Select
                  showSearch
                  placeholder={intl.formatMessage({ id: 'request.create.typeToSearch' })}
                  filterOption={false}
                  onSearch={handlePartSearch}
                  onSelect={handlePartSelect}
                  loading={partSearchLoading}
                  options={partsOptions}
                  notFoundContent={partSearchLoading ? intl.formatMessage({ id: 'request.create.searching' }) : intl.formatMessage({ id: 'request.create.typeToSearch' })}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item name="supplierSearch" label={intl.formatMessage({ id: 'request.create.form.searchSupplier' })}>
                <Select
                  showSearch
                  placeholder={intl.formatMessage({ id: 'request.create.typeToSearch' })}
                  filterOption={false}
                  onSearch={handleSupplierSearch}
                  onSelect={handleSupplierSelect}
                  loading={supplierSearchLoading}
                  options={supplierOptions}
                  notFoundContent={supplierSearchLoading ? intl.formatMessage({ id: 'request.create.searching' }) : intl.formatMessage({ id: 'request.create.typeToSearch' })}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={6}>
              <Form.Item name="partNumber" label={intl.formatMessage({ id: 'request.create.form.partNumber' })}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="partName" label={intl.formatMessage({ id: 'request.create.form.partName' })}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="eco" label={intl.formatMessage({ id: 'request.create.form.eco' })}>
                <Input />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item name="supplierName" label={intl.formatMessage({ id: 'request.create.form.supplierName' })}>
                <Input />
              </Form.Item>
            </Col>
          </Row>
        </Card>

        <Card title={intl.formatMessage({ id: 'request.create.requestDetails' })} style={{ marginBottom: 16 }}>
          <Form.Item name="requestReason" label={intl.formatMessage({ id: 'request.create.form.requestReason' })} rules={[{ required: true }]}>
            <TextArea rows={4} />
          </Form.Item>
        </Card>

        <Card title={intl.formatMessage({ id: 'request.create.analysisItems' })} style={{ marginBottom: 16 }}>
          <Form.Item name="analysisItems" label={intl.formatMessage({ id: 'request.create.form.selectItems' })} rules={[{ required: true }]}>
            <Cascader
              style={{ width: '100%' }}
              options={cascadeData}
              multiple
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
                { title: intl.formatMessage({ id: 'request.create.table.item' }), dataIndex: 'name' },
                { title: intl.formatMessage({ id: 'request.create.table.standards' }), dataIndex: 'testStandards' },
                { title: intl.formatMessage({ id: 'request.create.table.cost' }), dataIndex: 'cost', render: (v) => v ?? '-' },
                { title: intl.formatMessage({ id: 'request.create.table.unitPrice' }), dataIndex: 'unitPrice', render: (v) => v ?? '-' },
                { title: intl.formatMessage({ id: 'request.create.table.unit' }), dataIndex: 'unit', render: (v) => v ?? '-' },
              ]}
              summary={() => (
                <Table.Summary.Row>
                  <Table.Summary.Cell index={0} colSpan={3}>
                    <Text strong>{intl.formatMessage({ id: 'request.create.totalCost' })}</Text>
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
            {intl.formatMessage({ id: 'request.create.submit' })}
          </Button>
          <Button onClick={() => history.back()}>{intl.formatMessage({ id: 'common.cancel' })}</Button>
        </Space>
      </Form>
    </PageContainer>
  );
};

export default RequestCreate;
