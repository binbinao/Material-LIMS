import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getEquipmentRepairs, getEquipments } from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import EquipmentRepairs from '../index';

describe('EquipmentRepairs buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (getEquipmentRepairs as jest.Mock).mockResolvedValue({
      code: 200,
      data: { records: [{ id: 'rep-001', equipmentId: 'eq-001', status: 'REPAIRING' }], total: 1 },
    });
    (getEquipments as jest.Mock).mockResolvedValue({
      code: 200,
      data: { records: [{ id: 'eq-001', name: 'GC-MS', serialNumber: 'SN1' }] },
    });
  });

  it('Report Repair button opens create modal', async () => {
    const user = userEvent.setup();
    renderWithProviders(<EquipmentRepairs />);
    await waitFor(() => expect(screen.getByRole('button', { name: /Report Repair/i })).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /Report Repair/i }));
    await waitFor(() => expect(screen.getByText('Report Equipment Repair')).toBeInTheDocument());
  });

  it('renders Complete and Delete actions', async () => {
    renderWithProviders(<EquipmentRepairs />);
    await waitFor(() => {
      expect(screen.getByText('Complete')).toBeInTheDocument();
      expect(screen.getByText('Delete')).toBeInTheDocument();
    });
  });
});
