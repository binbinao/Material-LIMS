import React from 'react';
import { render, screen } from '@testing-library/react';
import { useParams } from '@umijs/max';
import { useRequest } from 'ahooks';
import ReportEdit from '../index';

jest.mock('@umijs/max', () => ({
  PageContainer: ({ children }: { children?: React.ReactNode }) => <div>{children}</div>,
  useParams: jest.fn(),
  history: { push: jest.fn() },
  useIntl: () => ({ formatMessage: ({ id }: { id: string }) => id }),
  useAccess: () => ({}),
  App: { useApp: () => ({ message: { error: jest.fn() }, modal: { confirm: jest.fn() } }) },
}));
jest.mock('ahooks', () => ({
  useRequest: jest.fn(),
}));
jest.mock('@/services/requestService', () => ({
  getReportEditUrl: jest.fn(),
  getReport: jest.fn(),
}));

type MockFn = jest.Mock;

describe('<ReportEdit />', () => {
  it('renders empty-state when editUrl is null', () => {
    (useParams as unknown as MockFn).mockReturnValue({ id: 'rpt-1' });
    (useRequest as unknown as MockFn)
      .mockReturnValueOnce({ data: { data: {} } })             // getReport
      .mockReturnValueOnce({ data: { data: null }, loading: false }); // getReportEditUrl
    render(<ReportEdit />);
    expect(screen.getByText('report.edit.unavailable')).toBeInTheDocument();
  });

  it('renders iframe and mock hint when editUrl is mock', () => {
    (useParams as unknown as MockFn).mockReturnValue({ id: 'rpt-2' });
    const mockUrl = 'https://example.invalid/mock-sharepoint/rpt-2?action=edit';
    (useRequest as unknown as MockFn)
      .mockReturnValueOnce({ data: { data: { versionNumber: 'V1.0' } } })
      .mockReturnValueOnce({ data: { data: mockUrl }, loading: false });
    render(<ReportEdit />);
    expect(screen.getByTitle('report.edit.title')).toHaveAttribute('src', mockUrl);
    expect(screen.getByText('report.edit.mockHint')).toBeInTheDocument();
  });
});