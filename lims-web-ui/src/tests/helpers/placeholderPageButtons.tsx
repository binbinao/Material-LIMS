import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ComponentType } from 'react';
import { renderWithProviders } from './render';

export function testPlaceholderPageCreateButton(pageName: string, Component: ComponentType) {
  describe(`${pageName} buttons`, () => {
    it('renders and clicks Create toolbar button without crashing', async () => {
      const user = userEvent.setup();
      renderWithProviders(<Component />);
      await waitFor(() => expect(screen.getByText('common.create')).toBeInTheDocument());
      await user.click(screen.getByText('common.create'));
    });
  });
}
