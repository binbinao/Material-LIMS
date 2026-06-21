import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import UserMenu from '../index';
import { renderWithProviders } from '@/tests/helpers/render';
import { logout } from '../../../utils/auth';
import * as antd from 'antd';

// Mock the auth utility so we can assert whether logout was called.
jest.mock('../../../utils/auth', () => ({
  logout: jest.fn(),
}));

describe('<UserMenu /> Logout confirmation', () => {
  const initialState = {
    currentUser: { displayName: '张三', roles: 'ROLE_REQUESTER' },
  };

  beforeEach(() => {
    (logout as jest.Mock).mockReset();
    jest.spyOn(antd.Modal, 'confirm').mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  test('clicking Logout opens Modal.confirm with correct options', async () => {
    const spy = antd.Modal.confirm as jest.Mock;
    renderWithProviders(<UserMenu initialState={initialState} />);
    await userEvent.click(screen.getByRole('button', { name: /张三/ }));
    await screen.findByText('Logout');
    await userEvent.click(screen.getByText('Logout'));

    // Modal.confirm should have been called with correct options
    expect(spy).toHaveBeenCalledTimes(1);
    expect(spy).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '确认退出登录？',
        okText: '退出',
        cancelText: '取消',
        okButtonProps: { danger: true },
      }),
    );
    // logout must not have been called yet
    expect(logout).not.toHaveBeenCalled();
  });

  test('canceling the modal does not call logout', async () => {
    const spy = antd.Modal.confirm as jest.Mock;
    let capturedOnCancel: (() => void) | undefined;
    spy.mockImplementation((opts: { onCancel?: () => void }) => {
      capturedOnCancel = opts.onCancel;
    });

    renderWithProviders(<UserMenu initialState={initialState} />);
    await userEvent.click(screen.getByRole('button', { name: /张三/ }));
    await screen.findByText('Logout');
    await userEvent.click(screen.getByText('Logout'));

    expect(spy).toHaveBeenCalled();
    // Simulate user clicking Cancel — logout is NOT called
    if (capturedOnCancel) {
      capturedOnCancel();
    }
    expect(logout).not.toHaveBeenCalled();
  });

  test('confirming the modal calls logout', async () => {
    const spy = antd.Modal.confirm as jest.Mock;
    let capturedOnOk: (() => void | Promise<void>) | undefined;
    spy.mockImplementation((opts: { onOk?: () => void | Promise<void> }) => {
      capturedOnOk = opts.onOk;
    });

    renderWithProviders(<UserMenu initialState={initialState} />);
    await userEvent.click(screen.getByRole('button', { name: /张三/ }));
    await screen.findByText('Logout');
    await userEvent.click(screen.getByText('Logout'));

    expect(spy).toHaveBeenCalled();
    // Simulate user clicking OK
    if (capturedOnOk) {
      await capturedOnOk!();
    }
    expect(logout).toHaveBeenCalledTimes(1);
  });
});
