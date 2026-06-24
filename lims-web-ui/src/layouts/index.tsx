import React from 'react';
import { useLocation } from '@umijs/max';
import CustomLayout from './CustomLayout';

interface LayoutWrapperProps {
  children: React.ReactNode;
}

/**
 * 布局包装器
 * 根据路由条件决定是否使用自定义布局
 * 登录页面不使用自定义布局（不显示退出按钮）
 */
export default function LayoutWrapper({ children }: LayoutWrapperProps) {
  const location = useLocation();
  
  // 登录页面不使用自定义布局
  const isLoginPage = location.pathname.startsWith('/login');
  
  if (isLoginPage) {
    return <>{children}</>;
  }
  
  return (
    <CustomLayout>
      {children}
    </CustomLayout>
  );
}