import { logout, isLogoutInProgress, silentLogout, forceLogout } from '../auth';
import { logout as mockLogoutFn } from '@/services/requestService';

// jest.setup.ts already mocks @/services/requestService globally.
// We override just the logout export here, using a factory that closes
// over a module-level jest.fn() so the test can control its behaviour.
// jest.mock is hoisted, so the factory must not reference externally-
// declared consts (TDZ).  We instead capture the fn from the factory
// return value and cast it.

const mockLogoutRequest = (mockLogoutFn as unknown) as jest.Mock;

const mockLocationHref = jest.fn();

// Mock window object
global.window = {
  ...global.window,
  location: {
    href: mockLocationHref,
  },
  localStorage: {
    getItem: jest.fn(),
    setItem: jest.fn(),
    removeItem: jest.fn(),
    clear: jest.fn(),
  },
  sessionStorage: {
    clear: jest.fn(),
  },
} as any;

describe('Auth Utilities', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    // Reset module state
    jest.resetModules();
  });

  describe('logout', () => {
    it('should perform logout successfully', async () => {
      mockLogoutRequest().mockResolvedValueOnce({});

      await logout();

      expect(mockLogoutRequest()).toHaveBeenCalledTimes(1);
      expect(window.localStorage.removeItem).toHaveBeenCalledWith('dev_user');
      expect(window.sessionStorage.clear).toHaveBeenCalled();
      expect(mockLocationHref).toHaveBeenCalledWith('/login');
    });

    it('should handle logout request failure gracefully', async () => {
      mockLogoutRequest().mockRejectedValueOnce(new Error('Network error'));

      await logout();

      expect(mockLogoutRequest()).toHaveBeenCalledTimes(1);
      expect(window.localStorage.removeItem).toHaveBeenCalledWith('dev_user');
      expect(window.sessionStorage.clear).toHaveBeenCalled();
      expect(mockLocationHref).toHaveBeenCalledWith('/login');
    });

    it('should prevent concurrent logout operations', async () => {
      mockLogoutRequest().mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));

      // Start first logout
      const logoutPromise1 = logout();
      
      // Try second logout immediately
      const logoutPromise2 = logout();

      await logoutPromise1;
      await logoutPromise2;

      // Only one logout request should be made
      expect(mockLogoutRequest()).toHaveBeenCalledTimes(1);
    });

    it('should clean up temporary storage data', async () => {
      mockLogoutRequest().mockResolvedValueOnce({});
      
      // Mock localStorage keys
      Object.defineProperty(window.localStorage, 'length', { value: 3 });
      Object.defineProperty(window.localStorage, 'key', {
        value: jest.fn((index) => {
          const keys = ['temp_session_data', 'session_cache', 'persistent_data'];
          return keys[index];
        }),
      });

      await logout();

      expect(window.localStorage.removeItem).toHaveBeenCalledWith('dev_user');
      expect(window.sessionStorage.clear).toHaveBeenCalled();
    });
  });

  describe('isLogoutInProgress', () => {
    it('should return false when no logout is in progress', () => {
      expect(isLogoutInProgress()).toBe(false);
    });

    it('should return true when logout is in progress', async () => {
      mockLogoutRequest().mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));

      const logoutPromise = logout();
      
      expect(isLogoutInProgress()).toBe(true);
      
      await logoutPromise;
      expect(isLogoutInProgress()).toBe(false);
    });
  });

  describe('silentLogout', () => {
    it('should perform logout without redirecting', async () => {
      mockLogoutRequest().mockResolvedValueOnce({});

      await silentLogout();

      expect(mockLogoutRequest()).toHaveBeenCalledTimes(1);
      expect(window.localStorage.removeItem).toHaveBeenCalledWith('dev_user');
      expect(window.sessionStorage.clear).toHaveBeenCalled();
      expect(mockLocationHref).not.toHaveBeenCalled();
    });

    it('should handle errors silently', async () => {
      mockLogoutRequest().mockRejectedValueOnce(new Error('Network error'));

      await silentLogout();

      expect(mockLogoutRequest()).toHaveBeenCalledTimes(1);
      expect(window.localStorage.removeItem).toHaveBeenCalledWith('dev_user');
      expect(window.sessionStorage.clear).toHaveBeenCalled();
    });
  });

  describe('forceLogout', () => {
    it('should immediately redirect to login page', () => {
      forceLogout();

      expect(window.localStorage.removeItem).toHaveBeenCalledWith('dev_user');
      expect(window.sessionStorage.clear).toHaveBeenCalled();
      expect(mockLocationHref).toHaveBeenCalledWith('/login');
    });
  });

  describe('server-side rendering', () => {
    it('should handle SSR environment gracefully', async () => {
      // Simulate SSR environment
      delete (global as any).window;

      await expect(logout()).resolves.not.toThrow();
      await expect(silentLogout()).resolves.not.toThrow();
      
      // Restore window for other tests
      global.window = {
        location: { href: mockLocationHref },
        localStorage: { removeItem: jest.fn() },
        sessionStorage: { clear: jest.fn() },
      } as any;
    });
  });
});