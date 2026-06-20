import { Button, Modal, Form, Input, InputNumber, TreeSelect, App, Card, Space, Dropdown, Tree } from 'antd';
import { PlusOutlined, MoreOutlined, EditOutlined, DeleteOutlined, ApartmentOutlined } from '@ant-design/icons';
import { useEffect, useMemo, useState } from 'react';
import { useAccess, useIntl } from '@umijs/max';
import { getDepartmentTree, createDepartment, updateDepartment, deleteDepartment } from '@/services/requestService';
import type { DataNode } from 'antd/es/tree';

type DeptNode = {
  id: string;
  name: string;
  parentId?: string | null;
  level?: number;
  externalId?: string;
  sortOrder?: number;
  children?: DeptNode[];
};

/**
 * /basic-data/departments page, refactored from a flat ProTable to a
 * tree view (antd Tree + Card). The backend already exposes
 * GET /api/v1/departments/tree returning the full nested structure
 * (see DepartmentService.tree). CRUD still goes through the existing
 * flat endpoints — we just flatten the tree into a `flatById` map
 * for the parent picker and pass `parentId` as the tree edge.
 *
 * Why a custom tree instead of ProTable's tree table:
 *   * Department hierarchy is the primary visual; a tree widget
 *     matches the mental model and saves horizontal space.
 *   * Each node needs an action menu (edit / add child / delete)
 *     which is cleaner as a Tree `titleRender` with a dropdown.
 *   * A flat list with `parentId` columns is harder to scan than
 *     a 5-level indentation at a glance.
 */
const DepartmentList: React.FC = () => {
  const access = useAccess();
  const intl = useIntl();
  const { message, modal } = App.useApp();
  const [form] = Form.useForm();
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<DeptNode | null>(null);
  const [treeData, setTreeData] = useState<DeptNode[]>([]);
  const [loading, setLoading] = useState(false);
  // A version counter so the tree `expandedKeys` + `selectedKeys`
  // survive reloads and any node edits stay highlighted.
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);

  // Only admin can mutate. Other roles get a read-only tree.
  const canEdit = !!access.canAdmin;

  const reload = async () => {
    setLoading(true);
    try {
      const res = await getDepartmentTree();
      const data: DeptNode[] = res?.data ?? [];
      setTreeData(data);
      // Expand all by default on first load (small org chart, < 50 nodes).
      setExpandedKeys(collectAllIds(data));
    } catch (e: any) {
      message.error(e?.message || 'Load failed');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    reload();
  }, []);

  // Flatten the tree into id → node so the create/edit modal can
  // pick a parent without re-walking the tree.
  const flatById = useMemo(() => {
    const map = new Map<string, DeptNode>();
    const walk = (nodes: DeptNode[]) => {
      for (const n of nodes) {
        map.set(n.id, n);
        if (n.children?.length) walk(n.children);
      }
    };
    walk(treeData);
    return map;
  }, [treeData]);

  // TreeSelect data — exclude the node being edited (and its
  // descendants) to prevent cycles in the parent picker.
  const parentTreeData = useMemo<DataNode[]>(() => {
    const blocked = new Set<string>();
    if (editing) {
      collectAllIds(editing.children ?? [], blocked);
      blocked.add(editing.id);
    }
    return toTreeSelectData(treeData, blocked);
  }, [treeData, editing]);

  const openCreate = (parentId?: string) => {
    setEditing(null);
    form.resetFields();
    if (parentId) form.setFieldValue('parentId', parentId);
    setModalOpen(true);
  };

  const openEdit = (node: DeptNode) => {
    setEditing(node);
    form.setFieldsValue({
      name: node.name,
      parentId: node.parentId ?? undefined,
      sortOrder: node.sortOrder,
      externalId: node.externalId,
    });
    setModalOpen(true);
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editing) {
        await updateDepartment(editing.id, values);
        message.success('Updated');
      } else {
        await createDepartment(values);
        message.success('Created');
      }
      setModalOpen(false);
      form.resetFields();
      setEditing(null);
      reload();
    } catch (e: any) {
      if (e?.errorFields) return; // AntD form validation
      message.error(e?.message || 'Save failed');
    }
  };

  const handleDelete = (node: DeptNode) => {
    const hasChildren = (node.children?.length ?? 0) > 0;
    modal.confirm({
      title: `Delete "${node.name}"?`,
      content: hasChildren
        ? 'This department has sub-departments. Deleting it will detach them from the tree (their parent_id will be cleared on next reload — but the rows themselves are not removed).'
        : undefined,
      okType: 'danger',
      onOk: async () => {
        await deleteDepartment(node.id);
        message.success('Deleted');
        reload();
      },
    });
  };

  // Convert backend tree → antd Tree data with title + actions menu.
  const treeNodes = useMemo<DataNode[]>(() => {
    return treeData.map((n) => buildTreeNode(n, canEdit, openCreate, openEdit, handleDelete));
  }, [treeData, canEdit]);

  return (
    <Card
      title={
        <Space>
          <ApartmentOutlined />
          Department Tree
          {treeData.length > 0 && (
            <span style={{ color: '#999', fontWeight: 'normal', fontSize: 13 }}>
              {countAll(treeData)} department(s)
            </span>
          )}
        </Space>
      }
      extra={
        canEdit && (
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate()}>
            {intl.formatMessage({ id: 'common.create' })}
          </Button>
        )
      }
      loading={loading}
    >
      {treeData.length === 0 && !loading ? (
        <div style={{ color: '#999', padding: 24, textAlign: 'center' }}>No departments yet.</div>
      ) : (
        <Tree
          treeData={treeNodes}
          expandedKeys={expandedKeys}
          onExpand={(keys) => setExpandedKeys(keys)}
          showLine
          blockNode
          defaultExpandAll={false}
        />
      )}

      <Modal
        title={editing ? 'Edit Department' : 'Create Department'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => { setModalOpen(false); form.resetFields(); setEditing(null); }}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Name" rules={[{ required: true, message: 'Name is required' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="parentId" label="Parent Department">
            <TreeSelect
              treeData={parentTreeData}
              placeholder="Top-level department"
              allowClear
              treeDefaultExpandAll
            />
          </Form.Item>
          <Form.Item name="sortOrder" label="Sort Order">
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="externalId" label="External ID">
            <Input placeholder="Optional ERP / external system id" />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

// ─── helpers ────────────────────────────────────────────────────────

function buildTreeNode(
  n: DeptNode,
  canEdit: boolean,
  onAddChild: (parentId: string) => void,
  onEdit: (n: DeptNode) => void,
  onDelete: (n: DeptNode) => void,
): DataNode {
  return {
    key: n.id,
    title: (
      <Space>
        <span style={{ fontWeight: 500 }}>{n.name}</span>
        {n.externalId && <span style={{ color: '#999', fontSize: 12 }}>({n.externalId})</span>}
        {canEdit && (
          <Dropdown
            trigger={['click']}
            menu={{
              items: [
                { key: 'add', label: 'Add sub-department', icon: <PlusOutlined />, onClick: () => onAddChild(n.id) },
                { key: 'edit', label: 'Edit', icon: <EditOutlined />, onClick: () => onEdit(n) },
                { type: 'divider' as const },
                { key: 'delete', label: 'Delete', icon: <DeleteOutlined />, danger: true, onClick: () => onDelete(n) },
              ],
            }}
          >
            <Button type="text" size="small" icon={<MoreOutlined />} />
          </Dropdown>
        )}
      </Space>
    ),
    children: n.children?.map((c) => buildTreeNode(c, canEdit, onAddChild, onEdit, onDelete)),
  };
}

function toTreeSelectData(nodes: DeptNode[], blocked: Set<string>): DataNode[] {
  return nodes
    .filter((n) => !blocked.has(n.id))
    .map((n) => ({
      title: n.name,
      value: n.id,
      key: n.id,
      children: n.children?.length ? toTreeSelectData(n.children, blocked) : undefined,
    }));
}

function collectAllIds(nodes: DeptNode[], out: Set<string> = new Set()): string[] {
  for (const n of nodes) {
    out.add(n.id);
    if (n.children?.length) collectAllIds(n.children, out);
  }
  return Array.from(out);
}

function countAll(nodes: DeptNode[]): number {
  return nodes.reduce((sum, n) => sum + 1 + (n.children ? countAll(n.children) : 0), 0);
}

export default DepartmentList;
