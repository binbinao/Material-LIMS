import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getHolidays } from '@/services/requestService';
import { testCrudPageButtons } from '@/tests/helpers/crudPageButtons';
import HolidayList from '../index';

testCrudPageButtons({
  pageName: 'HolidayList',
  Component: HolidayList,
  mockList: getHolidays as jest.Mock,
  listData: [{ id: 'hol-001', name: 'New Year', date: '2026-01-01', type: 'NATIONAL', year: 2026 }],
  createOpensModalTitle: 'Add Holiday',
});

describe('HolidayList Delete button', () => {
  beforeEach(() => {
    (getHolidays as jest.Mock).mockResolvedValue({
      code: 200,
      data: { records: [{ id: 'hol-001', name: 'New Year', date: '2026-01-01', type: 'NATIONAL', year: 2026 }], total: 1 },
    });
  });

  it('renders Delete action', async () => {
    const { renderWithProviders } = await import('@/tests/helpers/render');
    renderWithProviders(<HolidayList />);
    await waitFor(() => expect(screen.getByText('Delete')).toBeInTheDocument());
  });
});
