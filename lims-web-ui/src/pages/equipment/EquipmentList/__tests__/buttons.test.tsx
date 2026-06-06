import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { request } from '@umijs/max';
import { renderWithProviders } from '@/tests/helpers/render';
import EquipmentList from '../index';

describe('EquipmentList buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (request as jest.Mock).mockResolvedValue({
      code: 200,
      data: { records: [{ id: 'eq-001', name: 'GC-MS', status: 'ACTIVE', model: 'X1' }], total: 1 },
    });
  });

  it('Create button opens modal', async () => {
    const user = userEvent.setup();
    renderWithProviders(<EquipmentList />);
    await waitFor(() => expect(screen.getByText('Create')).toBeInTheDocument());
    await user.click(screen.getByText('Create'));
    await waitFor(() => expect(screen.getByText('Create Equipment')).toBeInTheDocument());
  });

  it('Edit action opens edit modal', async () => {
    const user = userEvent.setup();
    renderWithProviders(<EquipmentList />);
    await waitFor(() => expect(screen.getByText('Edit')).toBeInTheDocument());
    await user.click(screen.getByText('Edit'));
    await waitFor(() => expect(screen.getByText('Edit Equipment')).toBeInTheDocument());
  });

  it('Change Status calls API', async () => {
    const user = userEvent.setup();
    renderWithProviders(<EquipmentList />);
    await waitFor(() => expect(screen.getByText('Change Status')).toBeInTheDocument());
    await user.click(screen.getByText('Change Status'));
    await waitFor(() =>
      expect(request).toHaveBeenCalledWith('/api/v1/equipments/eq-001/status', expect.objectContaining({ method: 'PATCH' })),
    );
  });
});
