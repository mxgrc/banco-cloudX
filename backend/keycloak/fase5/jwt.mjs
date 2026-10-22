import { createPublicKey, verify } from 'node:crypto';

export function validateJwt(token, keys, { issuer, audience, nonce, access = false, now = Date.now() / 1000 }) {
  if (typeof token !== 'string' || token.length > 65536) throw Error('JWT ausente o demasiado grande');
  const parts = token.split('.');
  if (parts.length !== 3 || parts.some(p => !/^[A-Za-z0-9_-]+$/.test(p))) throw Error('Formato JWT inválido');
  const header = JSON.parse(Buffer.from(parts[0], 'base64url'));
  if (header.alg !== 'RS256' || typeof header.kid !== 'string' || header.crit) throw Error('Cabecera JWT no admitida');
  const candidates = keys.filter(k => k.kid === header.kid && k.kty === 'RSA' && k.use === 'sig' && (!k.alg || k.alg === 'RS256'));
  if (candidates.length !== 1) throw Error('Clave de firma no encontrada o ambigua');
  if (!verify('RSA-SHA256', Buffer.from(`${parts[0]}.${parts[1]}`), createPublicKey({ key: candidates[0], format: 'jwk' }), Buffer.from(parts[2], 'base64url'))) throw Error('Firma inválida');
  const claims = JSON.parse(Buffer.from(parts[1], 'base64url'));
  if (claims.iss !== issuer) throw Error('Issuer incorrecto');
  const audiences = Array.isArray(claims.aud) ? claims.aud : [claims.aud];
  if (!audiences.includes(audience)) throw Error('Audience incorrecta');
  if (!Number.isFinite(claims.exp) || claims.exp <= now) throw Error('Token expirado o sin exp');
  if (!Number.isFinite(claims.iat) || claims.iat > now + 30) throw Error('iat inválido');
  if (claims.nbf !== undefined && (!Number.isFinite(claims.nbf) || claims.nbf > now + 30)) throw Error('Token aún no vigente');
  if (typeof claims.sub !== 'string' || !claims.sub) throw Error('Falta sujeto autenticado');
  if (nonce !== undefined && claims.nonce !== nonce) throw Error('Nonce incorrecto');
  if (access && claims.typ !== 'Bearer') throw Error('Se requiere access token Bearer de Keycloak');
  return claims;
}
