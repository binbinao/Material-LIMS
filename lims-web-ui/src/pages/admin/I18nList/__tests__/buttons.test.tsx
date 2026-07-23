import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getI18nMessages } from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import I18nList from '../index';

describe('I18nList buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (getI18nMessages as jest.Mock).mockImplementation((locale: string) =>
      Promise.resolve({
        data: locale === 'zh-CN' ? { 'common.create': '创建' } : { 'common.create': 'Create' },
      }),
    );
  });

  it('Create button opens modal', async () => {
    const user = userEvent.setup();
    renderWithProviders(<I18nList />);
    await waitFor(() => expect(screen.getByText('Create')).toBeInTheDocument());
    await user.click(screen.getByText('Create'));
    await waitFor(() => expect(screen.getByText('Add Translation')).toBeInTheDocument());
  });

  it('renders Edit and Delete actions', async () => {
    renderWithProviders(<I18nList />);
    await waitFor(() => {
      expect(screen.getByText('Edit')).toBeInTheDocument();
      expect(screen.getByText('Delete')).toBeInTheDocument();
    });
  });
});
