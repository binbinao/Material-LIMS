import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getRequests } from '@/services/requestService';
import { mockHistory } from '@/tests/setup';
import { renderWithProviders } from '@/tests/helpers/render';
import RequestList from '../index';

describe('RequestList buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (getRequests as jest.Mock).mockResolvedValue({
      code: 200,
      data: { records: [{ id: 'req-001', requestNo: 'REQ-2026-0001', status: 'DRAFT', priority: 'NORMAL' }], total: 1 },
    });
  });

  it('Create button navigates to create page', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RequestList />);
    await waitFor(() => expect(screen.getByRole('button', { name: /Create/i })).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /Create/i }));
    expect(mockHistory.push).toHaveBeenCalledWith('/request/create');
  });
});
