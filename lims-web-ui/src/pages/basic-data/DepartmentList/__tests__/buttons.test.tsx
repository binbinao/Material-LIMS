import { getDepartments } from '@/services/requestService';
import { testCrudPageButtons } from '@/tests/helpers/crudPageButtons';
import DepartmentList from '../index';

testCrudPageButtons({
  pageName: 'DepartmentList',
  Component: DepartmentList,
  mockList: getDepartments as jest.Mock,
  listDataShape: 'array',
  listData: [{ id: 'dept-001', name: 'QA', level: 1, sortOrder: 1 }],
  createOpensModalTitle: 'Create Department',
  editRecord: { id: 'dept-001', name: 'QA' },
  editOpensModalTitle: 'Edit Department',
});
