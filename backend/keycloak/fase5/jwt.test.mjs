import { test } from 'node:test';
import assert from 'node:assert/strict';
import { generateKeyPairSync, sign } from 'node:crypto';
import { validateJwt } from './jwt.mjs';

const pair = generateKeyPairSync('rsa', { modulusLength: 2048 });
const key = { ...pair.publicKey.export({ format: 'jwk' }), kid: 'test', use: 'sig', alg: 'RS256' };
const base = { iss: 'http://issuer.test', aud: ['api'], sub: 'user-test', exp: 200, iat: 90, nbf: 90, typ: 'Bearer' };
const options = { issuer: base.iss, audience: 'api', access: true, now: 100 };
function token(claims = {}, header = {}) {
  const data = [ { alg: 'RS256', kid: 'test', ...header }, { ...base, ...claims } ].map(v => Buffer.from(JSON.stringify(v)).toString('base64url')).join('.');
  return data + '.' + sign('RSA-SHA256', Buffer.from(data), pair.privateKey).toString('base64url');
}
test('acepta firma RSA y claims válidos', () => assert.equal(validateJwt(token(), [key], options).sub, base.sub));
for (const [name, claims] of Object.entries({ issuer: {iss:'otro'}, audience: {aud:'otro'}, expirado:{exp:99}, sinExp:{exp:null}, futuro:{nbf:150}, iatFuturo:{iat:150}, idToken:{typ:'ID'}, sinSub:{sub:''} })) {
  test(`rechaza ${name} con firma auténtica`, () => assert.throws(() => validateJwt(token(claims), [key], options)));
}
test('rechaza payload alterado sin volver a firmar', () => {
  const parts = token().split('.');
  parts[1] = Buffer.from(JSON.stringify({...base, sub:'intruso'})).toString('base64url');
  assert.throws(() => validateJwt(parts.join('.'), [key], options), /Firma/);
});
test('rechaza algoritmo inesperado', () => assert.throws(() => validateJwt(token({}, {alg:'none'}), [key], options)));
test('rechaza kid desconocido', () => assert.throws(() => validateJwt(token({}, {kid:'otra'}), [key], options)));
test('rechaza nonce incorrecto', () => assert.throws(() => validateJwt(token({nonce:'a'}), [key], {...options, nonce:'b'})));
test('acepta ID token solo con audiencia frontend y nonce esperado', () => assert.equal(validateJwt(token({typ:'ID', aud:'frontend', nonce:'a'}), [key], {...options, audience:'frontend', access:false, nonce:'a'}).nonce, 'a'));
