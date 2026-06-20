import { defineConfig } from '@umijs/max';
import React from 'react';
import routes from './config/routes';
import UserMenu from './src/components/UserMenu';

export default defineConfig({
  antd: {},
  access: {},
  model: {},
  initialState: {},
  locale: {},
  request: {},
  layout: {
    title: 'Material LIMS',
    locale: true,
    actionsRender: (props: any) => [
      React.createElement(UserMenu, {
        key: 'user-menu',
        initialState: props?.initialState,
      }),
    ],
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
