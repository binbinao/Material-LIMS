import { request } from '@umijs/max';
import { renderWithProviders } from '@/tests/helpers/render';
import RequestDashboard from '../index';

describe('RequestDashboard', () => {
  beforeEach(() => {
    (request as jest.Mock).mockResolvedValue({
      code: 200,
      data: { byStatus: { DRAFT: 1 }, byBrand: { 'brand-001': 1 }, total: 1 },
    });
  });

  it('renders without interactive buttons (read-only dashboard)', () => {
    const { container } = renderWithProviders(<RequestDashboard />);
    expect(container.querySelectorAll('button').length).toBe(0);
  });
});
