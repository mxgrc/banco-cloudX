// Prueba HTTP sin credenciales. Requiere server.mjs y Keycloak en ejecución.
import assert from 'node:assert/strict';
const origin = 'http://localhost:4200';
const home = await fetch(origin);
assert.equal(home.status, 200);
assert.equal(home.headers.get('cache-control'), 'no-store');
assert.equal(home.headers.get('referrer-policy'), 'strict-origin');
const cookie = home.headers.get('set-cookie').split(';')[0];
const html = await home.text();
const csrf = /name="csrf" value="([^"]+)"/.exec(html)[1];
const rejected = await fetch(origin+'/login', {method:'POST',headers:{Cookie:cookie,Origin:'http://otro.test'},body:new URLSearchParams({csrf}),redirect:'manual'});
assert.equal(rejected.status,400);
for (const suppliedOrigin of ['null', undefined]) {
  const headers = {Cookie:cookie};
  if (suppliedOrigin !== undefined) headers.Origin = suppliedOrigin;
  const invalidOrigin = await fetch(origin+'/login', {method:'POST',headers,body:new URLSearchParams({csrf}),redirect:'manual'});
  assert.equal(invalidOrigin.status,400);
}
const badCsrf = await fetch(origin+'/login', {method:'POST',headers:{Cookie:cookie,Origin:origin},body:new URLSearchParams({csrf:'incorrecto'}),redirect:'manual'});
assert.equal(badCsrf.status,400);
const login = await fetch(origin+'/login', {method:'POST',headers:{Cookie:cookie,Origin:origin},body:new URLSearchParams({csrf}),redirect:'manual'});
assert.equal(login.status,303);
const target = new URL(login.headers.get('location'));
assert.equal(target.origin,'http://localhost:8080');
assert.equal(target.searchParams.get('client_id'),'banco-cloud-frontend');
assert.equal(target.searchParams.get('redirect_uri'),origin+'/');
assert.equal(target.searchParams.get('code_challenge_method'),'S256');
assert.equal(target.searchParams.get('code_challenge').length,43);
assert.ok(target.searchParams.get('nonce'));
const kc = await fetch(target, {redirect:'manual'});
assert.equal(kc.status,200, 'Keycloak debe aceptar la petición OIDC y presentar login');
const loginHtml = await kc.text();
assert.match(loginHtml,/type="password"/);
const invalid = await fetch(origin+'/?code=falso&state=incorrecto',{headers:{Cookie:cookie},redirect:'manual'});
assert.equal(invalid.status,303);
const after = await fetch(origin+'/',{headers:{Cookie:cookie}});
assert.match(await after.text(),/State ausente, vencido o incorrecto/);
console.log('PASS HTTP: página 200, origen ajeno rechazado, redirección OIDC con S256/nonce, login Keycloak 200 y callback con state inválido rechazado.');
console.log('Pendiente: el usuario debe completar el login real; no se usaron contraseñas en esta prueba.');
