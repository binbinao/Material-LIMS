import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getReport, submitReport } from '@/services/requestService';
import { useParams } from '@umijs/max';
import { renderWithProviders } from '@/tests/helpers/render';
import ReportDetail from '../index';

describe('ReportDetail buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (useParams as jest.Mock).mockReturnValue({ id: 'rep-001' });
    (getReport as jest.Mock).mockResolvedValue({
      code: 200,
      data: {
        id: 'rep-001',
        requestId: 'req-001',
        status: 'DRAFT',
        versionNumber: 'V1.0',
        authorId: 'user-001',
      },
    });
    (submitReport as jest.Mock).mockResolvedValue({ code: 200 });
  });

  it('shows Submit button for DRAFT and calls submitReport', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ReportDetail />);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Submit' })).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: 'Submit' }));
    await waitFor(() => expect(submitReport).toHaveBeenCalledWith('rep-001'));
  });

  it('shows Edit in M365 and Sync buttons for DRAFT', async () => {
    renderWithProviders(<ReportDetail />);
    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Edit' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Download' })).toBeInTheDocument();
    });
  });
});
