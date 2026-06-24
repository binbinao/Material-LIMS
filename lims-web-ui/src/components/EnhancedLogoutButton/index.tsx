import React, { useState } from 'react';
import { useModel } from '@umijs/max';
import { message } from 'antd';
import LogoutButton from '../LogoutButton';
import LogoutConfirmModal from '../LogoutConfirmModal';

/**
 * 增强的退出按钮组件
 * 集成确认对话框功能，提供完整的退出体验
 */
export default function EnhancedLogoutButton() {
  const { initialState } = useModel('@@initialState');
  const [confirmVisible, setConfirmVisible] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleLogoutClick = () => {
    setConfirmVisible(true);
  };

  const handleCancel = () => {
    setConfirmVisible(false);
  };

  const handleConfirm = async () => {
    setLoading(true);
    
    try {
      const { logout, isLogoutInProgress } = await import('../../utils/auth');
      
      // 检查是否正在退出中
      if (isLogoutInProgress()) {
        message.warning('退出操作正在进行中，请稍候');
        return;
      }
      
      await logout();
      
      // 成功提示（虽然用户可能看不到，但有助于调试）
      message.success('退出成功，正在跳转到登录页...');
      
    } catch (error) {
      console.error('退出失败:', error);
      message.error('退出失败，请重试');
      
      // 如果退出失败，使用强制退出
      const { forceLogout } = await import('../../utils/auth');
      forceLogout();
      
    } finally {
      setLoading(false);
      setConfirmVisible(false);
    }
  };

  return (
    <>
      <LogoutButton 
        floating={true}
        size="middle"
        onLogout={handleLogoutClick}
      />
      
      <LogoutConfirmModal
        visible={confirmVisible}
        onCancel={handleCancel}
        onConfirm={handleConfirm}
        currentUser={initialState?.currentUser}
        loading={loading}
      />
    </>
  );
}