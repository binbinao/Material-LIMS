import React, { useState } from 'react';
import { Modal, Button, Space, Typography } from 'antd';
import { LogoutOutlined, ExclamationCircleOutlined } from '@ant-design/icons';
import styles from './index.module.less';

const { Text } = Typography;

interface LogoutConfirmModalProps {
  /** 是否显示模态框 */
  visible: boolean;
  /** 关闭模态框回调 */
  onCancel: () => void;
  /** 确认退出回调 */
  onConfirm: () => void;
  /** 当前用户信息 */
  currentUser?: any;
  /** 加载状态 */
  loading?: boolean;
}

/**
 * 退出确认对话框组件
 * 提供Material Design风格的退出确认体验，包含用户信息和安全提示
 */
export default function LogoutConfirmModal({
  visible,
  onCancel,
  onConfirm,
  currentUser,
  loading = false
}: LogoutConfirmModalProps) {
  
  const handleConfirm = () => {
    onConfirm();
  };

  return (
    <Modal
      open={visible}
      onCancel={onCancel}
      footer={null}
      className={styles.modal}
      centered
      closable={false}
      maskClosable={true}
    >
      <div className={styles.content}>
        <div className={styles.iconContainer}>
          <ExclamationCircleOutlined className={styles.warningIcon} />
        </div>
        
        <div className={styles.textContainer}>
          <Text strong className={styles.title}>
            确认退出系统？
          </Text>
          
          {currentUser && (
            <Text type="secondary" className={styles.userInfo}>
              当前用户：{currentUser.displayName || currentUser.username}
            </Text>
          )}
          
          <Text type="secondary" className={styles.description}>
            退出后需要重新登录才能访问系统。请确保已保存所有工作。
          </Text>
        </div>
        
        <Space size="middle" className={styles.actions}>
          <Button 
            onClick={onCancel}
            className={styles.cancelButton}
            disabled={loading}
          >
            取消
          </Button>
          <Button
            type="primary"
            danger
            icon={<LogoutOutlined />}
            onClick={handleConfirm}
            loading={loading}
            className={styles.confirmButton}
          >
            确认退出
          </Button>
        </Space>
      </div>
    </Modal>
  );
}