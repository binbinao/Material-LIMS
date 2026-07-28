import { screen, waitFor } from '@testing-library/react';
import { getAdminUsers } from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import UserList from '../index';

describe('UserList buttons', () => {
  beforeEach(() => {
    (getAdminUsers as jest.Mock).mockResolvedValue({
      code: 200,
      data: {
        records: [{ id: 'user-001', displayName: 'Admin', email: 'a@test.com', roles: 'ADMIN', isActive: true }],
        total: 1,
      },
    });
  });

  it('renders Edit Roles action', async () => {
    renderWithProviders(<UserList />);
    await waitFor(() => expect(screen.getByText('Edit User Roles')).toBeInTheDocument());
  });
});
