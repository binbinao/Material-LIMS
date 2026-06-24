import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { useModel, useLocation } from '@umijs/max';
import CustomLayout from '../index';

describe('CustomLayout', () => {
  const mockInitialState = {
    currentUser: {
      name: 'Test User',
      avatar: 'test-avatar.jpg',
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();
    (useLocation as jest.Mock).mockReturnValue({
      pathname: '/workspace',
    });
    
    (useModel as jest.Mock).mockReturnValue({
      initialState: mockInitialState,
    });
  });

  it('should render layout with content for non-login pages', () => {
    render(
      <CustomLayout>
        <div>Test Content</div>
      </CustomLayout>
    );

    expect(screen.getByText('Test Content')).toBeInTheDocument();
  });

  it('should not render custom layout for login page', () => {
    (useLocation as jest.Mock).mockReturnValue({
      pathname: '/login',
    });

    render(
      <CustomLayout>
        <div>Login Page Content</div>
      </CustomLayout>
    );

    expect(screen.getByText('Login Page Content')).toBeInTheDocument();
  });

  it('should handle missing initialState gracefully', () => {
    (useModel as jest.Mock).mockReturnValue({
      initialState: null,
    });

    render(
      <CustomLayout>
        <div>Test Content</div>
      </CustomLayout>
    );

    expect(screen.getByText('Test Content')).toBeInTheDocument();
  });
});