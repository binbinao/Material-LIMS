import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/tests/helpers/render';
import RequestNoteList from '../index';

describe('RequestNoteList buttons', () => {
  it('renders Create toolbar button', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RequestNoteList />);
    await waitFor(() => expect(screen.getByText('common.create')).toBeInTheDocument());
    await user.click(screen.getByText('common.create'));
    // Placeholder page: no modal yet, but button should be clickable without error
  });
});
