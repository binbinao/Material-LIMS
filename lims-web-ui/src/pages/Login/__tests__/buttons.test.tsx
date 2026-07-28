import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { login } from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import Login from '../index';

// jest.mock is hoisted — factory self-contained, no external refs
jest.mock('@/services/requestService', () => ({
  login: jest.fn(),
}));

describe('Login buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders Sign in button and calls login on submit', async () => {
    const user = userEvent.setup();
    (login as jest.Mock).mockResolvedValue({
      code: 200,
      data: { user: { id: 'u-1', displayName: 'admin' }, token: 'jwt' },
    });

    renderWithProviders(<Login />);
    const btn = screen.getByRole('button', { name: /Sign in/i });
    expect(btn).toBeInTheDocument();

    await user.click(btn);
    await waitFor(() => expect(login).toHaveBeenCalled());
  });
});
