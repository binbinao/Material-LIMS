import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  getRequest,
  getRequestTasks,
  getRequestWorkflow,
  submitRequest,
} from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import RequestDetail from '../index';

const draftRequest = {
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
    (getRequest as jest.Mock).mockResolvedValue({ code: 200, data: draftRequest });
    (getRequestTasks as jest.Mock).mockResolvedValue({ code: 200, data: [] });
    (getRequestWorkflow as jest.Mock).mockResolvedValue({ code: 200, data: { tasks: [] } });
    (submitRequest as jest.Mock).mockResolvedValue({ code: 200 });
  });

  it('shows Submit button for DRAFT status and calls submitRequest', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RequestDetail />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Submit' })).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: 'Submit' }));
    await waitFor(() => expect(submitRequest).toHaveBeenCalledWith('req-001'));
  });
});
