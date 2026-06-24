import { defineConfig } from '@umijs/max';
import routes from './config/routes';

export default defineConfig({
  antd: {},
  access: {},
  model: {},
  initialState: {},
  locale: {},
  request: {},
  // 移除layout配置，使用自定义布局组件
  // layout: false,
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
