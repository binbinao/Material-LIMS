import { render, type RenderOptions } from '@testing-library/react';
import { App, ConfigProvider } from 'antd';
import React from 'react';

export function renderWithProviders(ui: React.ReactElement, options?: RenderOptions) {
  const Wrapper = ({ children }: { children: React.ReactNode }) => (
    <ConfigProvider>
      <App>{children}</App>
    </ConfigProvider>
  );
  return render(ui, { wrapper: Wrapper, ...options });
}
