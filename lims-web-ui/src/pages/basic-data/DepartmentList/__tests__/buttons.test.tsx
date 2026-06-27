import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getDepartmentTree } from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import DepartmentList from '../index';

describe('DepartmentList buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders Create toolbar button and opens modal', async () => {
    const user = userEvent.setup();
    (getDepartmentTree as jest.Mock).mockResolvedValue({
      code: 200,
      data: [{ id: 'dept-001', name: 'QA', level: 1, sortOrder: 1, children: [] }],
    });

    renderWithProviders(<DepartmentList />);
    await waitFor(() => expect(screen.getByText('common.create')).toBeInTheDocument());
    await user.click(screen.getByText('common.create'));
    await waitFor(() => expect(screen.getByText('Create Department')).toBeInTheDocument());
  });

  it('renders department tree with data', async () => {
    (getDepartmentTree as jest.Mock).mockResolvedValue({
      code: 200,
      data: [{ id: 'dept-001', name: 'QA', level: 1, sortOrder: 1, children: [] }],
    });

    renderWithProviders(<DepartmentList />);
    await waitFor(() => expect(screen.getByText('QA')).toBeInTheDocument());
  });
});