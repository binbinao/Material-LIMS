import { getCostStats } from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import CostDashboard from '../index';

describe('CostDashboard', () => {
  beforeEach(() => {
    (getCostStats as jest.Mock).mockResolvedValue({
      code: 200,
      data: { totalCost: 1000, requestCount: 2, costByBrand: { 'Brand A': 1000 } },
    });
  });

  it('renders without interactive buttons (read-only dashboard)', () => {
    const { container } = renderWithProviders(<CostDashboard />);
    expect(container.querySelectorAll('button').length).toBe(0);
  });
});
