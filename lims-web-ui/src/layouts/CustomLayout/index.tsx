import React from 'react';
import { ProLayout } from '@ant-design/pro-components';
import { useModel } from '@umijs/max';
import UserMenu from '../../components/UserMenu';
import EnhancedLogoutButton from '../../components/EnhancedLogoutButton';
import styles from './index.module.less';

interface CustomLayoutProps {
  children: React.ReactNode;
  [key: string]: any;
}

/**
 * 自定义布局组件
 * 集成左下角退出按钮和原有的用户菜单
 * 提供Material Design风格的完整界面体验
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
        
        {/* 左下角退出按钮 */}
        <EnhancedLogoutButton />
      </ProLayout>
    </div>
  );
}