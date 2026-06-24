/**
 * Auth utilities — kept out of app.tsx because Umi treats every top-level
 * export in app.tsx as a plugin key. Putting logout in a separate file
 * avoids the "register failed, invalid key logout" runtime error.
 */

import { logout as logoutRequest } from '@/services/requestService';

// 退出状态管理
let isLoggingOut = false;

/**
 * 检查是否正在退出中
 */
export function isLogoutInProgress(): boolean {
  return isLoggingOut;
}

/**
 * Logout — three steps:
 *   1. POST /api/v1/auth/logout so the server sets LIMS_TOKEN cookie
 *      to MaxAge=0 (the browser drops it).
 *   2. Clear the dev-login marker from localStorage so a future
 *      requestInterceptor (app.tsx) doesn't keep sending X-Dev-User.
 *   3. Force a full reload to /login so getInitialState() re-runs
 *      anonymously.
 *
 * Step 1 is best-effort: if the network is down or the server is
 * unreachable, we still want to get the user to the login screen.
 * The server-side cookie stays valid until natural expiry (TTL
 * hours, see JwtTokenProvider), but the user is no longer in the
 * app's working session.
 */
export async function logout(): Promise<void> {
  // 防止重复退出
  if (isLoggingOut) {
    console.warn('退出操作正在进行中，请勿重复操作');
    return;
  }
  
  isLoggingOut = true;
  
  try {
    if (typeof window !== 'undefined') {
      // 添加退出前的清理工作
      const cleanupTasks = [
        // 清除开发用户标记
        () => window.localStorage.removeItem('dev_user'),
        // 清除其他可能的会话数据
        () => window.sessionStorage.clear(),
        // 清除临时存储
        () => {
          Object.keys(window.localStorage).forEach(key => {
            if (key.startsWith('temp_') || key.startsWith('session_')) {
              window.localStorage.removeItem(key);
            }
          });
        }
      ];
      
      // 执行清理任务
      cleanupTasks.forEach(task => {
        try {
          task();
        } catch (error) {
          console.warn('清理任务执行失败:', error);
        }
      });
      
      // 发送退出请求
      try {
        await logoutRequest();
      } catch (error) {
        // Network error or non-2xx — we don't care. The user is logging
        // out; the worst case is the cookie is still valid until expiry.
        console.warn('退出请求失败，继续执行退出流程:', error);
      }
      
      // 延迟跳转，确保清理完成
      setTimeout(() => {
        window.location.href = '/login';
      }, 100);
    }
  } catch (error) {
    console.error('退出过程中发生错误:', error);
    // 确保即使出错也能跳转到登录页
    if (typeof window !== 'undefined') {
      window.location.href = '/login';
    }
  } finally {
    isLoggingOut = false;
  }
}

/**
 * 静默退出 - 不跳转页面，仅清理本地数据
 */
export async function silentLogout(): Promise<void> {
  if (typeof window !== 'undefined') {
    try {
      await logoutRequest();
    } catch {
      // 忽略错误
    }
    
    // 仅清理本地数据，不跳转
    window.localStorage.removeItem('dev_user');
    window.sessionStorage.clear();
  }
}

/**
 * 强制退出 - 忽略所有错误，直接跳转
 */
export function forceLogout(): void {
  if (typeof window !== 'undefined') {
    // 快速清理
    window.localStorage.removeItem('dev_user');
    window.sessionStorage.clear();
    
    // 立即跳转
    window.location.href = '/login';
  }
}
