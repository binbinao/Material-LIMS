import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getReports } from '@/services/requestService';
import { mockHistory } from '@/tests/setup';
import { renderWithProviders } from '@/tests/helpers/render';
import ReportArchive from '../index';

describe('ReportArchive links', () => {
  beforeEach(() => {
    (getReports as jest.Mock).mockResolvedValue({
      code: 200,
      data: { records: [{ id: 'rep-001', requestId: 'req-001', status: 'APPROVED', versionNumber: 'V1.0' }], total: 1 },
    });
  });

  it('View link navigates to report detail', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReportArchive />);
    await waitFor(() => expect(screen.getByText('View')).toBeInTheDocument());
    await user.click(screen.getByText('View'));
    expect(mockHistory.push).toHaveBeenCalledWith('/report/rep-001');
  });
});
