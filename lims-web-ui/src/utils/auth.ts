/**
 * Auth utilities — kept out of app.tsx because Umi treats every top-level
 * export in app.tsx as a plugin key. Putting logout in a separate file
 * avoids the "register failed, invalid key logout" runtime error.
 */

import { logout as logoutRequest } from '@/services/requestService';

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
export async function logout() {
  if (typeof window !== 'undefined') {
    try {
      await logoutRequest();
    } catch {
      // Network error or non-2xx — we don't care. The user is logging
      // out; the worst case is the cookie is still valid until expiry.
    }
    window.localStorage.removeItem('dev_user');
  }
  window.location.href = '/login';
}
