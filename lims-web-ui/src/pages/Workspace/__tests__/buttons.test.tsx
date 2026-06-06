import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getMyDashboard, getMyPendingTasks } from '@/services/requestService';
import { mockHistory } from '@/tests/setup';
import { renderWithProviders } from '@/tests/helpers/render';
import Workspace from '../index';

describe('Workspace buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (getMyDashboard as jest.Mock).mockResolvedValue({
      code: 200,
      data: { requestStats: { DRAFT: 1, SUBMITTED: 2 }, pendingTasks: 1, overdue: 0 },
    });
    (getMyPendingTasks as jest.Mock).mockResolvedValue({
      code: 200,
      data: [{ taskName: 'Review', requestId: 'req-001' }],
    });
  });

  it('navigates to request list when Draft card clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Workspace />);
    await waitFor(() => expect(screen.getByText('Draft Requests')).toBeInTheDocument());
    await user.click(screen.getByText('Draft Requests').closest('.ant-card')!);
    expect(mockHistory.push).toHaveBeenCalledWith('/request/list');
  });

  it('navigates to kanban when View All clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Workspace />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'View All' })).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: 'View All' }));
    expect(mockHistory.push).toHaveBeenCalledWith('/request/kanban');
  });

  it('navigates to request detail when View link clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Workspace />);
    await waitFor(() => expect(screen.getByText('View')).toBeInTheDocument());
    await user.click(screen.getByText('View'));
    expect(mockHistory.push).toHaveBeenCalledWith('/request/req-001');
  });
});
