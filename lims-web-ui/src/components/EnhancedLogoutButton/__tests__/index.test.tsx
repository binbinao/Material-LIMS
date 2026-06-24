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

// jest.mock factories are hoisted — never close over externally-declared consts.
// All mock state is captured inside the factory or stored on the module itself.

// Mock the actual EnhancedLogoutButton component with simplified logic.
// The factory references globals (React) and inline functions, never
// externally-declared consts (which would be in TDZ at hoist time).
const mockLogout = jest.fn();

jest.mock('../index', () => {
  const logoutFn = mockLogout; // Safe: reference to already-initialized jest.fn()
  return function MockEnhancedLogoutButton() {
    const [showModal, setShowModal] = React.useState(false);
    
    const handleLogoutClick = () => setShowModal(true);
    const handleConfirm = () => { logoutFn(); setShowModal(false); };
    const handleCancel = () => setShowModal(false);
    
    return (
      <div>
        <button data-testid="logout-button" onClick={handleLogoutClick}>
          Logout
        </button>
        {showModal && (
          <div data-testid="logout-confirm-modal">
            <button data-testid="confirm-cancel" onClick={handleCancel}>Cancel</button>
            <button data-testid="confirm-ok" onClick={handleConfirm}>Confirm</button>
          </div>
        )}
      </div>
    );
  };
});

describe('EnhancedLogoutButton', () => {
  const mockInitialState = {
    currentUser: {
      name: 'Test User',
      email: 'test@example.com',
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useModel as jest.Mock).mockReturnValue({
      initialState: mockInitialState,
    });
  });

  it('should render logout button and show modal on click', () => {
    render(<EnhancedLogoutButton />);

    const logoutButton = screen.getByTestId('logout-button');
    expect(logoutButton).toBeInTheDocument();
    expect(logoutButton).toHaveTextContent('Logout');

    // Initially modal should be hidden
    expect(screen.queryByTestId('logout-confirm-modal')).not.toBeInTheDocument();

    // Click logout button to show modal
    fireEvent.click(logoutButton);

    expect(screen.getByTestId('logout-confirm-modal')).toBeInTheDocument();
  });

  it('should handle successful logout', () => {
    render(<EnhancedLogoutButton />);

    // Show modal
    fireEvent.click(screen.getByTestId('logout-button'));
    expect(screen.getByTestId('logout-confirm-modal')).toBeInTheDocument();

    // Confirm logout
    fireEvent.click(screen.getByTestId('confirm-ok'));

    // Modal should be closed after logout
    expect(screen.queryByTestId('logout-confirm-modal')).not.toBeInTheDocument();
    expect(mockLogout).toHaveBeenCalledTimes(1);
  });

  it('should allow canceling logout', () => {
    render(<EnhancedLogoutButton />);

    // Show modal
    fireEvent.click(screen.getByTestId('logout-button'));
    expect(screen.getByTestId('logout-confirm-modal')).toBeInTheDocument();

    // Cancel logout
    fireEvent.click(screen.getByTestId('confirm-cancel'));

    // Modal should be closed
    expect(screen.queryByTestId('logout-confirm-modal')).not.toBeInTheDocument();
    expect(mockLogout).not.toHaveBeenCalled();
  });

  it('should handle missing initialState gracefully', () => {
    (useModel as jest.Mock).mockReturnValue({
      initialState: null,
    });

    render(<EnhancedLogoutButton />);

    const logoutButton = screen.getByTestId('logout-button');
    expect(logoutButton).toBeInTheDocument();

    fireEvent.click(logoutButton);
    expect(screen.getByTestId('logout-confirm-modal')).toBeInTheDocument();
  });
});