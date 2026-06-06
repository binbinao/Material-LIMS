import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getAdminLogs } from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import LogList from '../index';

describe('LogList buttons', () => {
  beforeEach(() => {
    (getAdminLogs as jest.Mock).mockResolvedValue({
      code: 200,
      data: {
        records: [{ id: 'log-001', module: 'REQUEST', action: 'CREATE', userId: 'user-001', userName: 'Dev', createdAt: '2026-06-06' }],
        total: 1,
      },
    });
  });

  it('View action opens detail modal', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LogList />);
    await waitFor(() => expect(screen.getByText('View')).toBeInTheDocument());
    await user.click(screen.getByText('View'));
    await waitFor(() => expect(screen.getByText('Audit Log Detail')).toBeInTheDocument());
  });
});
