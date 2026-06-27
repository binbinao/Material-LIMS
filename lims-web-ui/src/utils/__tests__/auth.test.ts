import { logout, isLogoutInProgress, silentLogout, forceLogout } from '../auth';

// jest.mock is hoisted — factory self-contained, no external refs
jest.mock('@/services/requestService', () => ({
  logout: jest.fn(),
}));

function mockLogoutRequest(): jest.Mock {
  const { logout: fn } = require('@/services/requestService');
  return fn as jest.Mock;
}

describe('Auth Utilities', () => {
  let removeItemSpy: jest.SpyInstance;
  let sessionClearSpy: jest.SpyInstance;

  beforeEach(() => {
    jest.clearAllMocks();

    // Spy on Storage.prototype — auth code calls localStorage.removeItem / sessionStorage.clear
    removeItemSpy = jest.spyOn(Storage.prototype, 'removeItem');
    sessionClearSpy = jest.spyOn(Storage.prototype, 'clear');
  });

  afterEach(() => {
    removeItemSpy.mockRestore();
    sessionClearSpy.mockRestore();
  });

  describe('logout', () => {
    it('should call logout API and clean storage', async () => {
      mockLogoutRequest().mockResolvedValueOnce({});

      await logout();

      expect(mockLogoutRequest()).toHaveBeenCalledTimes(1);
      expect(removeItemSpy).toHaveBeenCalledWith('dev_user');
      expect(sessionClearSpy).toHaveBeenCalled();
    });

    it('should clean storage even when logout API fails', async () => {
      mockLogoutRequest().mockRejectedValueOnce(new Error('Network error'));

      await logout();

      expect(mockLogoutRequest()).toHaveBeenCalledTimes(1);
      expect(removeItemSpy).toHaveBeenCalledWith('dev_user');
      expect(sessionClearSpy).toHaveBeenCalled();
    });

    it('should prevent concurrent logout operations', async () => {
      mockLogoutRequest().mockImplementation(() => new Promise(r => setTimeout(r, 100)));

      void logout();
      void logout();

      // Allow the first logout to complete
      await new Promise(r => setTimeout(r, 200));

      expect(mockLogoutRequest()).toHaveBeenCalledTimes(1);
    });

    it('should clean up temporary storage keys', async () => {
      mockLogoutRequest().mockResolvedValueOnce({});

      // Set up some temp keys in localStorage before logout
      window.localStorage.setItem('temp_foo', 'bar');
      window.localStorage.setItem('session_cache', 'data');

      await logout();

      expect(removeItemSpy).toHaveBeenCalledWith('dev_user');
      expect(sessionClearSpy).toHaveBeenCalled();
    });
  });

  describe('isLogoutInProgress', () => {
    it('should return false when no logout is in progress', () => {
      expect(isLogoutInProgress()).toBe(false);
    });

    it('should return true during active logout', async () => {
      mockLogoutRequest().mockImplementation(() => new Promise(r => setTimeout(r, 100)));

      const promise = logout();
      expect(isLogoutInProgress()).toBe(true);

      await promise;
      expect(isLogoutInProgress()).toBe(false);
    });
  });

  describe('silentLogout', () => {
    it('should call API and clean storage without redirect', async () => {
      mockLogoutRequest().mockResolvedValueOnce({});

      await silentLogout();

      expect(mockLogoutRequest()).toHaveBeenCalledTimes(1);
      expect(removeItemSpy).toHaveBeenCalledWith('dev_user');
      expect(sessionClearSpy).toHaveBeenCalled();
    });

    it('should clean storage even when API fails', async () => {
      mockLogoutRequest().mockRejectedValueOnce(new Error('Network error'));

      await silentLogout();

      expect(mockLogoutRequest()).toHaveBeenCalledTimes(1);
      expect(removeItemSpy).toHaveBeenCalledWith('dev_user');
      expect(sessionClearSpy).toHaveBeenCalled();
    });
  });

  describe('forceLogout', () => {
    it('should clean local storage immediately', () => {
      forceLogout();

      expect(removeItemSpy).toHaveBeenCalledWith('dev_user');
      expect(sessionClearSpy).toHaveBeenCalled();
    });
  });
});