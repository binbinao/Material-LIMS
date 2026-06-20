/**
 * Auth utilities — kept out of app.tsx because Umi treats every top-level
 * export in app.tsx as a plugin key. Putting logout in a separate file
 * avoids the "register failed, invalid key logout" runtime error.
 */

/**
 * Logout — clears the dev-login marker from localStorage so the next
 * request goes through as anonymous, then redirects to /login. For
 * production this would also call POST /api/v1/auth/logout to invalidate
 * the JWT server-side.
 */
export function logout() {
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem('dev_user');
  }
  // Force a full reload so getInitialState() re-runs as anonymous.
  window.location.href = '/login';
}
