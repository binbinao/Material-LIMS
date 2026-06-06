import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getReports } from '@/services/requestService';
import { mockHistory } from '@/tests/setup';
import { renderWithProviders } from '@/tests/helpers/render';
import ReportList from '../index';

describe('ReportList links', () => {
  beforeEach(() => {
    (getReports as jest.Mock).mockResolvedValue({
      code: 200,
      data: { records: [{ id: 'rep-001', requestId: 'req-001', status: 'DRAFT', versionNumber: 'V1.0' }], total: 1 },
    });
  });

  it('report id link navigates to detail', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReportList />);
    await waitFor(() => expect(screen.getByText('rep-001...')).toBeInTheDocument());
    await user.click(screen.getByText('rep-001...'));
    expect(mockHistory.push).toHaveBeenCalledWith('/report/rep-001');
  });
});
