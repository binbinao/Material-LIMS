// Umi runtime configuration
export const request = {
  timeout: 10000,
  errorConfig: {
    errorThrower() {
      // Disable default error throwing - we handle errors in components
    },
    errorHandler(error: any) {
      // Silently handle common errors - don't throw or redirect
      if (error?.response?.status === 401 || error?.response?.status === 403) {
        console.warn('Auth error:', error?.response?.status);
        return;
      }
      console.warn('Request error:', error?.message || 'Unknown error');
    },
  },
  requestInterceptors: [
    (config: any) => {
      // Add skipErrorHandler to all requests by default
      return { ...config, skipErrorHandler: true };
    },
  ],
  responseInterceptors: [],
};

export async function getInitialState(): Promise<{
  currentUser?: API.CurrentUser;
}> {
  try {
    const res = await fetch('/api/v1/auth/me');
    const json = await res.json();
    return { currentUser: json?.data };
  } catch {
    return {};
  }
}
