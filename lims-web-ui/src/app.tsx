// Umi runtime configuration
import { App as AntApp } from 'antd';
import React from 'react';

/**
 * Wrap the whole app in <AntApp> so that App.useApp() inside any page
 * returns the real { message, modal, notification } APIs (otherwise they
 * are non-functional placeholders and produce
 * `TypeError: message.error is not a function` in production builds).
 */
export function rootContainer(container: React.ReactNode) {
  return React.createElement(AntApp, { style: { height: '100%' } }, container);
}

export const request = {
  timeout: 10000,
  errorConfig: {
    errorThrower() {
      // Disable default error throwing - we handle errors in components
    },
    errorHandler(error: any) {
      // Issue #13: on 401, the JWT has expired or is otherwise invalid.
      // Redirect to /login so the user can re-authenticate instead of
      // being stuck on a page that silently fails every API call.
      if (error?.response?.status === 401) {
        if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/login')) {
          window.location.href = '/login';
        }
        return;
      }
      if (error?.response?.status === 403) {
        console.warn('Auth forbidden:', error?.response?.status);
        return;
      }
      console.warn('Request error:', error?.message || 'Unknown error');
    },
  },
  requestInterceptors: [
    (config: any) => {
      // Dev-only: DevAuthFilter reads the X-Dev-User header to synthesize
      // a principal without a real JWT. The username is stored in
      // localStorage by the Login page's "Dev Quick Login" form so it
      // survives page reloads. No-op in production (localStorage empty).
      const devUser = typeof window !== 'undefined'
        ? window.localStorage?.getItem('dev_user')
        : null;
      const headers = { ...(config?.headers || {}) };
      if (devUser && !headers['X-Dev-User']) {
        headers['X-Dev-User'] = devUser;
      }
      return { ...config, headers, skipErrorHandler: true };
    },
  ],
  responseInterceptors: [],
};

export async function getInitialState(): Promise<{
  currentUser?: API.CurrentUser;
}> {
  try {
    const devUser = typeof window !== 'undefined'
      ? window.localStorage?.getItem('dev_user')
      : null;
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (devUser) headers['X-Dev-User'] = devUser;
    const res = await fetch('/api/v1/auth/me', { headers, credentials: 'include' });
    // Issue #13: a 401 means the JWT is missing/expired. Treat that as
    // "not logged in" rather than as a successful response with a
    // possibly-malformed payload. Returning {} here leaves currentUser
    // undefined, so the access plugin marks the user as anonymous.
    if (res.status === 401) {
      return {};
    }
    const json = await res.json();
    return { currentUser: json?.data };
  } catch {
    return {};
  }
}

/**
 * Logout has been moved to src/utils/auth.ts. Umi treats every top-level
 * export of app.tsx as a plugin key — exporting a function named
 * `logout` triggers "register failed, invalid key logout" at boot.
 */
