import { defineConfig } from '@umijs/max';
import routes from './config/routes';

export default defineConfig({
  antd: {},
  access: {},
  model: {},
  initialState: {},
  locale: {
    default: 'zh-CN',
    antd: true,
    baseNavigator: false,
  },
  request: {},
  // 使用 Umi 内置 layout 插件 — side 布局。
  // rightContentRender 在此模式下渲染到侧边栏底部，UserMenu 将出现在那里。
  layout: {
    title: 'Material LIMS',
    locale: true,
    logo: 'https://gw.alipayobjects.com/zos/rmsportal/KDpgvguMpGfqaHPjicRK.svg',
    layout: 'side',
    contentWidth: 'Fluid',
    fixedHeader: true,
    fixSiderbar: true,
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