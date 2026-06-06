import { getAnalysisItems } from '@/services/requestService';
import { testCrudPageButtons } from '@/tests/helpers/crudPageButtons';
import AnalysisItemList from '../index';

testCrudPageButtons({
  pageName: 'AnalysisItemList',
  Component: AnalysisItemList,
  mockList: getAnalysisItems as jest.Mock,
  listData: [{ id: 'item-001', name: 'ICP Analysis', cost: 500, groupId: 'group-001', typeId: 'atype-001' }],
  createOpensModalTitle: 'Create Analysis Item',
  editRecord: { id: 'item-001', name: 'ICP Analysis' },
  editOpensModalTitle: 'Edit Analysis Item',
});
