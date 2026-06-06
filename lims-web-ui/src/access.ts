/**
 * Access control definitions
 * Used by Umi's access plugin to control route and element visibility
 */
export default function access(initialState: { currentUser?: API.CurrentUser } | undefined) {
  const { currentUser } = initialState ?? {};
  const roles = currentUser?.roles?.split(',') ?? [];

  return {
    canAdmin: roles.includes('ADMIN'),
    canManager: roles.includes('MANAGER') || roles.includes('ADMIN'),
    canEngineer: roles.includes('ENGINEER') || roles.includes('MANAGER') || roles.includes('ADMIN'),
    canTechnician: roles.includes('TECHNICIAN') || roles.includes('MANAGER') || roles.includes('ADMIN'),
  };
}
