import { getBrands } from '@/services/requestService';
import { testCrudPageButtons } from '@/tests/helpers/crudPageButtons';
import BrandList from '../index';

testCrudPageButtons({
  pageName: 'BrandList',
  Component: BrandList,
  mockList: getBrands as jest.Mock,
  listData: [{ id: 'brand-001', name: 'Brand A', description: 'Test', sortOrder: 1 }],
  createOpensModalTitle: 'Create Brand',
  editRecord: { id: 'brand-001', name: 'Brand A' },
  editOpensModalTitle: 'Edit Brand',
});
