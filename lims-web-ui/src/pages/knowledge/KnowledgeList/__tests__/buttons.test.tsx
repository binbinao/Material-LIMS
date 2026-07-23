import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { getKnowledgeDocs } from '@/services/requestService';
import { renderWithProviders } from '@/tests/helpers/render';
import KnowledgeList from '../index';

describe('KnowledgeList buttons', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (getKnowledgeDocs as jest.Mock).mockResolvedValue({
      code: 200,
      data: {
        records: [{ id: 'doc-001', title: 'SOP Manual', category: 'MANUAL', fileUrl: 'http://example.com/doc.pdf', fileSize: 1024 }],
        total: 1,
      },
    });
  });

  it('Upload button opens modal', async () => {
    const user = userEvent.setup();
    renderWithProviders(<KnowledgeList />);
    await waitFor(() => expect(screen.getByRole('button', { name: /Upload/i })).toBeInTheDocument());
    await user.click(screen.getByRole('button', { name: /Upload/i }));
    await waitFor(() => expect(screen.getByRole('dialog')).toHaveTextContent('Upload Knowledge Document'));
  });

  it('renders Delete action', async () => {
    renderWithProviders(<KnowledgeList />);
    await waitFor(() => expect(screen.getByText('Delete')).toBeInTheDocument());
  });
});
