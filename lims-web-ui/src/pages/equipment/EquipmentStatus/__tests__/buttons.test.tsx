import { getEquipmentStats, getEquipments } from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import EquipmentStatus from '../index';

describe('EquipmentStatus', () => {
  beforeEach(() => {
    (getEquipmentStats as jest.Mock).mockResolvedValue({ code: 200, data: { statusCounts: { ACTIVE: 1 } } });
    (getEquipments as jest.Mock).mockResolvedValue({ code: 200, data: { records: [] } });
  });

  it('renders without buttons (read-only page)', () => {
    const { container } = renderWithProviders(<EquipmentStatus />);
    expect(container.querySelectorAll('button').length).toBe(0);
  });
});
