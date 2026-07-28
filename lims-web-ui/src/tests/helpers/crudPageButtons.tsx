import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ComponentType } from 'react';
import { renderWithProviders } from './render';

type CrudPageButtonTestOptions = {
  pageName: string;
  Component: ComponentType;
  mockList: jest.Mock;
  listData: unknown[];
  /** DepartmentList returns `data` as a plain array; most ProTable pages use paginated records. */
  listDataShape?: 'array' | 'paginated';
  createOpensModalTitle: string;
  editRecord?: Record<string, unknown>;
  editOpensModalTitle?: string;
};

export function testCrudPageButtons({
  pageName,
  Component,
  mockList,
  listData,
  listDataShape = 'paginated',
  createOpensModalTitle,
  editRecord,
  editOpensModalTitle,
}: CrudPageButtonTestOptions) {
  describe(`${pageName} buttons`, () => {
    beforeEach(() => {
      jest.clearAllMocks();
      mockList.mockResolvedValue({
        code: 200,
        data: listDataShape === 'array' ? listData : { records: listData, total: listData.length },
      });
    });

    it('renders Create toolbar button and opens modal', async () => {
      const user = userEvent.setup();
      renderWithProviders(<Component />);

      await waitFor(() => expect(screen.getByText('Create')).toBeInTheDocument());
      await user.click(screen.getByText('Create'));
      await waitFor(() => expect(screen.getByText(createOpensModalTitle)).toBeInTheDocument());
    });

    if (editRecord && editOpensModalTitle) {
      it('renders Edit action and opens edit modal', async () => {
        const user = userEvent.setup();
        renderWithProviders(<Component />);

        await waitFor(() => expect(screen.getByText('Edit')).toBeInTheDocument());
        await user.click(screen.getByText('Edit'));
        await waitFor(() => expect(screen.getByText(editOpensModalTitle)).toBeInTheDocument());
      });
    }

    if (editRecord) {
      it('renders Delete action', async () => {
        renderWithProviders(<Component />);
        await waitFor(() => expect(screen.getByText('Delete')).toBeInTheDocument());
      });
    }
  });
}
