import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getReportRevisions } from '@/services/requestService';
import { mockHistory } from '@/tests/setup';
import { renderWithProviders } from '@/tests/helpers/render';
import ReportRevisions from '../index';

describe('ReportRevisions links', () => {
  beforeEach(() => {
    (getReportRevisions as jest.Mock).mockResolvedValue({
      code: 200,
      data: [{ id: 'rep-001', versionNumber: 'V1.0', status: 'APPROVED' }],
    });
  });

  it('version link navigates to report detail', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReportRevisions />);
    await waitFor(() => expect(screen.getByText('V1.0')).toBeInTheDocument());
    await user.click(screen.getByText('V1.0'));
    expect(mockHistory.push).toHaveBeenCalledWith('/report/rep-001');
  });
});
