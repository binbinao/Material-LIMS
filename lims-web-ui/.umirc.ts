import { defineConfig } from '@umijs/max';
import React from 'react';
import routes from './config/routes';
import pkg from './package.json';
import UserMenu from './src/components/UserMenu';
import VersionBadge from './src/components/VersionBadge';

export default defineConfig({
  antd: {},
  access: {},
  model: {},
  initialState: {},
  locale: {},
  request: {},
  // Inject APP_VERSION at build time. JSON.stringify keeps it a literal
  // string in the bundle so VersionBadge can read it as process.env.APP_VERSION.
  define: {
    'process.env.APP_VERSION': JSON.stringify(pkg.version),
  },
  layout: {
    title: 'Material LIMS',
    locale: true,
    // actionsRender items are laid out left-to-right in array order.
    // Badge first → it appears to the left of the user button.
    actionsRender: (props: any) => [
      React.createElement(VersionBadge, { key: 'version-badge' }),
      React.createElement(UserMenu, {
        key: 'user-menu',
        initialState: props?.initialState,
      }),
    ],
    // Defensive: if a future Umi upgrade changes how rightRender.tsx
    // decides whether to wrap the default avatar in a dropdown, our
    // logout still works because it goes through the same code path.
    logout: async () => {
      const { logout } = await import('./src/utils/auth');
      await logout();
    },
  },
  routes,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
  npmClient: 'npm',
  hash: true,
});

