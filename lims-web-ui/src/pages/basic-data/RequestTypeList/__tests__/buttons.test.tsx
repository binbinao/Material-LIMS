import { getRequestTypes } from '@/services/requestService';
import { testCrudPageButtons } from '@/tests/helpers/crudPageButtons';
import RequestTypeList from '../index';

testCrudPageButtons({
  pageName: 'RequestTypeList',
  Component: RequestTypeList,
  mockList: getRequestTypes as jest.Mock,
  listData: [{ id: 'type-001', name: 'Material Analysis', taskDurationDays: 10, active: true }],
  createOpensModalTitle: 'Create Request Type',
  editRecord: { id: 'type-001', name: 'Material Analysis' },
  editOpensModalTitle: 'Edit Request Type',
});
