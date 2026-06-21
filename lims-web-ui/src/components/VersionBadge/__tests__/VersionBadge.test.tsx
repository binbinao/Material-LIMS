import React from 'react';
import { render, screen, act, fireEvent } from '@testing-library/react';
import VersionBadge from '../index';

describe('<VersionBadge />', () => {
  test('renders the version from process.env.APP_VERSION, with 0.0.0 fallback', () => {
    // jest does not run the Umi `define` step, so process.env.APP_VERSION
    // is undefined here. The component should fall back to "0.0.0".
    render(<VersionBadge />);
    expect(screen.getByText('v0.0.0')).toBeInTheDocument();
  });

  test('renders BACKEND_VERSION and BUILD_DATE inside the tooltip', () => {
    // antd Tooltip uses setTimeout internally to delay portal rendering.
    // In jsdom we must use fake timers + act() to flush the pending timeout.
    jest.useFakeTimers();
    render(<VersionBadge />);

    const tag = screen.getByText('v0.0.0');
    fireEvent.mouseEnter(tag);

    act(() => {
      jest.runAllTimers();
    });

    // Tooltip content is now in the document body.
    expect(document.body.textContent).toContain('Backend 1.0.0-SNAPSHOT');
    expect(document.body.textContent).toContain('Build 2026-06-21');

    jest.useRealTimers();
  });
});
