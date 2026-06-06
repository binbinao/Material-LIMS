import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  getBrands,
  getRequestTypes,
  getAnalysisItemCascade,
  createRequest,
} from '@/services/requestService';
import { mockHistory } from '@/tests/setup';
import { renderWithProviders } from '@/tests/helpers/render';
import RequestCreate from '../index';

describe('RequestCreate buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (getBrands as jest.Mock).mockResolvedValue({
      code: 200,
      data: { records: [{ id: 'brand-001', name: 'Brand A' }] },
    });
    (getRequestTypes as jest.Mock).mockResolvedValue({
      code: 200,
      data: { records: [{ id: 'type-001', name: 'Material Analysis', taskDurationDays: 10 }] },
    });
    (getAnalysisItemCascade as jest.Mock).mockResolvedValue({ code: 200, data: [] });
  });

  it('renders Submit and Cancel buttons', () => {
    renderWithProviders(<RequestCreate />);
    expect(screen.getByRole('button', { name: 'Submit Request' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
  });

  it('Cancel navigates back', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RequestCreate />);
    await user.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(mockHistory.back).toHaveBeenCalled();
  });

  it('Submit shows validation error without analysis items', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RequestCreate />);
    await user.click(screen.getByRole('button', { name: 'Submit Request' }));
    expect(createRequest).not.toHaveBeenCalled();
  });
});
