import { getRequests } from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import RequestKanban from '../index';

describe('RequestKanban', () => {
  beforeEach(() => {
    (getRequests as jest.Mock).mockResolvedValue({ code: 200, data: { records: [], total: 0 } });
  });

  it('renders without buttons (kanban cards are not clickable)', () => {
    const { container } = renderWithProviders(<RequestKanban />);
    expect(container.querySelectorAll('button').length).toBe(0);
  });
});
