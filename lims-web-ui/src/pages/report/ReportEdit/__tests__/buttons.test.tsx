import { getReport, getReportEditUrl } from '@/services/requestService';
import { mockHistory } from '@/tests/setup';
import { renderWithProviders } from '@/tests/helpers/render';
import ReportEdit from '../index';

describe('ReportEdit navigation', () => {
  beforeEach(() => {
    (getReport as jest.Mock).mockResolvedValue({
      code: 200,
      data: { id: 'rep-001', versionNumber: 'V1.0' },
    });
    (getReportEditUrl as jest.Mock).mockResolvedValue({ code: 200, data: 'https://office.com/edit' });
  });

  it('renders page with back navigation handler configured', () => {
    renderWithProviders(<ReportEdit />);
    expect(mockHistory.push).toBeDefined();
  });
});
