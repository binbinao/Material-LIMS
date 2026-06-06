import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getAuthUrl } from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import Login from '../index';

describe('Login buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders SSO button and calls getAuthUrl on click', async () => {
    const user = userEvent.setup();
    (getAuthUrl as jest.Mock).mockResolvedValue({ data: 'https://login.microsoft.com/mock' });

    renderWithProviders(<Login />);
    const btn = screen.getByRole('button', { name: /Sign in with Microsoft 365/i });
    expect(btn).toBeInTheDocument();

    await user.click(btn);
    await waitFor(() => expect(getAuthUrl).toHaveBeenCalledTimes(1));
  });
});
