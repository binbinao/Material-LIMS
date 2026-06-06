import { defineConfig } from '@umijs/max';
import routes from './config/routes';

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
  },
  routes,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
  npmClient: 'npm',
  jsMinifier: 'terser',
  hash: true,
});
