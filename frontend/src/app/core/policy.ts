export type Role = 'ADMIN' | 'CLIENTE';
export function portalRole(claims: Record<string, unknown> | undefined): Role | null {
  const resource = claims?.['resource_access'] as Record<string, {roles?: unknown}> | undefined;
  const roles = resource?.['banco-cloud-api']?.roles;
  const scopes = typeof claims?.['scope'] === 'string' ? claims['scope'].split(' ') : [];
  if (!Array.isArray(roles) || !scopes.includes('banco-cloud-access')) return null;
  if (roles.includes('ADMIN') === roles.includes('CLIENTE')) return null;
  return roles.includes('ADMIN') ? 'ADMIN' : 'CLIENTE';
}
export function isApiRequest(url: string, origin: string, apiBase: string): boolean {
  const target = new URL(url, origin);
  const api = new URL(apiBase, origin);
  return target.origin === api.origin && target.pathname.startsWith(api.pathname.replace(/\/$/, '') + '/');
}
