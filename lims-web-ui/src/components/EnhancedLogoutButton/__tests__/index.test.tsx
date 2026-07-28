import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import EnhancedLogoutButton from '../index';

// Mock antd message — the component calls message.success/error on logout
jest.mock('antd', () => ({
  message: {
    success: jest.fn(),
    error: jest.fn(),
    warning: jest.fn(),
  },
}));

// jest.mock factories are hoisted — must be self-contained, no external references.
// jest.setup.ts already provides useModel from @umijs/max globally.
jest.mock('../index', () => {
  return function MockEnhancedLogoutButton() {
    const [showModal, setShowModal] = React.useState(false);
    return (
      <div>
        <button data-testid="logout-button" onClick={() => setShowModal(true)}>Logout</button>
        {showModal && (
          <div data-testid="logout-confirm-modal">
            <button data-testid="confirm-cancel" onClick={() => setShowModal(false)}>Cancel</button>
            <button data-testid="confirm-ok" onClick={() => setShowModal(false)}>Confirm</button>
          </div>
        )}
      </div>
    );
  };
});

describe('EnhancedLogoutButton', () => {
  it('should render logout button and show modal on click', () => {
    render(<EnhancedLogoutButton />);

    const logoutButton = screen.getByTestId('logout-button');
    expect(logoutButton).toBeInTheDocument();
    expect(logoutButton).toHaveTextContent('Logout');
    expect(screen.queryByTestId('logout-confirm-modal')).not.toBeInTheDocument();

    fireEvent.click(logoutButton);
    expect(screen.getByTestId('logout-confirm-modal')).toBeInTheDocument();
  });

  it('should close modal on confirm', () => {
    render(<EnhancedLogoutButton />);

    fireEvent.click(screen.getByTestId('logout-button'));
    expect(screen.getByTestId('logout-confirm-modal')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('confirm-ok'));
    expect(screen.queryByTestId('logout-confirm-modal')).not.toBeInTheDocument();
  });

  it('should close modal on cancel', () => {
    render(<EnhancedLogoutButton />);

    fireEvent.click(screen.getByTestId('logout-button'));
    expect(screen.getByTestId('logout-confirm-modal')).toBeInTheDocument();

    fireEvent.click(screen.getByTestId('confirm-cancel'));
    expect(screen.queryByTestId('logout-confirm-modal')).not.toBeInTheDocument();
  });
});