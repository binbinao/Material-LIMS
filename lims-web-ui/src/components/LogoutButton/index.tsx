import React from 'react';
import { Button, Tooltip } from 'antd';
import { LogoutOutlined } from '@ant-design/icons';
import styles from './index.module.less';

interface LogoutButtonProps {
  /** 是否显示为浮动按钮 */
  floating?: boolean;
  /** 按钮大小 */
  size?: 'small' | 'middle' | 'large';
  /** 点击回调函数 */
  onLogout?: () => void;
}

/**
 * 左下角退出按钮组件
 * 提供Material Design风格的退出功能，支持浮动定位和确认对话框
 */
export default function LogoutButton({ 
  floating = true, 
  size = 'middle',
  onLogout 
}: LogoutButtonProps) {
  
  const handleClick = async () => {
    if (onLogout) {
      onLogout();
      return;
    }
    
    // 默认退出逻辑
    const { logout } = await import('../../utils/auth');
    logout();
  };

  return (
    <div className={`${styles.logoutButton} ${floating ? styles.floating : ''}`}>
      <Tooltip 
        title="安全退出系统" 
        placement="right"
        overlayClassName={styles.tooltip}
      >
        <Button
          type="primary"
          danger
          size={size}
          icon={<LogoutOutlined />}
          onClick={handleClick}
          className={styles.button}
          aria-label="退出系统"
        >
          退出
        </Button>
      </Tooltip>
    </div>
  );
}