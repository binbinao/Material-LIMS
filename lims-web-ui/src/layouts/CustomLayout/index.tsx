import React from 'react';
import { ProLayout } from '@ant-design/pro-components';
import { useModel } from '@umijs/max';
import UserMenu from '../../components/UserMenu';
import styles from './index.module.less';

interface CustomLayoutProps {
  children: React.ReactNode;
  [key: string]: any;
}

/**
 * 自定义布局组件
 * 集成用户菜单到顶部操作栏。
 * 退出登录入口已统一迁移至 UserMenu 下拉菜单中。
 */
export default function CustomLayout({ children, ...props }: CustomLayoutProps) {
  const { initialState } = useModel('@@initialState');

  return (
    <div className={styles.customLayout}>
      <ProLayout
        {...props}
        title="Material LIMS"
        logo="https://gw.alipayobjects.com/zos/rmsportal/KDpgvguMpGfqaHPjicRK.svg"
        layout="mix"
        splitMenus
        fixedHeader
        fixSiderbar
        contentStyle={{ margin: 0 }}
        actionsRender={() => [
          <UserMenu key="user-menu" initialState={initialState} />
        ]}
      >
        {children}
      </ProLayout>
    </div>
  );
}