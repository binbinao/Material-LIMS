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
    await waitFor(() => expect(screen.getByText('common.create')).toBeInTheDocument());
    await user.click(screen.getByText('common.create'));
    await waitFor(() => expect(screen.getByText('Add Translation')).toBeInTheDocument());
  });

  it('renders Edit and Delete actions', async () => {
    renderWithProviders(<I18nList />);
    await waitFor(() => {
      expect(screen.getByText('common.edit')).toBeInTheDocument();
      expect(screen.getByText('common.delete')).toBeInTheDocument();
    });
  });
});
