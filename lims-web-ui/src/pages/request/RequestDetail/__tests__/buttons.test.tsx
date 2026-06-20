import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  getRequest,
  getRequestTasks,
  getRequestWorkflow,
  getAdminUsers,
  submitRequest,
  assignRequest,
} from '@/services/requestService';
import { useParams } from '@umijs/max';
import { renderWithProviders } from '@/tests/helpers/render';
import RequestDetail from '../index';

const baseRequest = {
  id: 'req-001',
  requestNo: 'REQ-2026-0001',
  status: 'DRAFT',
  priority: 'NORMAL',
  brandId: 'brand-001',
  typeId: 'type-001',
  requestReason: 'test',
};

describe('RequestDetail buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (useParams as jest.Mock).mockReturnValue({ id: 'req-001' });
    (getRequest as jest.Mock).mockResolvedValue({ code: 200, data: baseRequest });
    (getRequestTasks as jest.Mock).mockResolvedValue({ code: 200, data: [] });
    (getRequestWorkflow as jest.Mock).mockResolvedValue({ code: 200, data: { tasks: [] } });
    (getAdminUsers as jest.Mock).mockResolvedValue({
      code: 200,
      data: { records: [{ id: 'eng-1', displayName: 'Eng One', roles: 'ENGINEER' }] },
    });
    (submitRequest as jest.Mock).mockResolvedValue({ code: 200 });
    (assignRequest as jest.Mock).mockResolvedValue({ code: 200 });
  });

  it('shows Submit button for DRAFT status and calls submitRequest', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RequestDetail />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Submit' })).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: 'Submit' }));
    await waitFor(() => expect(submitRequest).toHaveBeenCalledWith('req-001'));
  });

  it('shows Assign and Reject buttons for SUBMITTED status', async () => {
    (getRequest as jest.Mock).mockResolvedValue({ code: 200, data: { ...baseRequest, status: 'SUBMITTED' } });
    renderWithProviders(<RequestDetail />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Assign' })).toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Reject' })).toBeInTheDocument();
  });

  it('blocks assignment when no engineer selected', async () => {
    const user = userEvent.setup();
    (getRequest as jest.Mock).mockResolvedValue({ code: 200, data: { ...baseRequest, status: 'SUBMITTED' } });
    (getRequestTasks as jest.Mock).mockResolvedValue({
      code: 200,
      data: [{ id: 'task-1', itemId: 'item-1', status: 'PENDING' }],
    });
    renderWithProviders(<RequestDetail />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Assign' })).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: 'Assign' }));
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Assign' }));
    await waitFor(() => expect(assignRequest).not.toHaveBeenCalled());
  });
});
