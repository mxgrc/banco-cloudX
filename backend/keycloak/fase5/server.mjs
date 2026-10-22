import http from 'node:http';
import { randomBytes, createHash } from 'node:crypto';
import { validateJwt } from './jwt.mjs';
import { businessPanel, executeAction } from './business.mjs';

const origin = 'http://localhost:4200';
const issuer = 'http://localhost:8080/realms/banco-cloud';
const client = 'banco-cloud-frontend';
const audience = 'banco-cloud-api';
const sessions = new Map();
const random = () => randomBytes(32).toString('base64url');
const escape = v => String(v).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));

async function jsonFetch(url, init = {}) {
  if (!url.startsWith(issuer + '/')) throw Error('Endpoint OIDC no confiable');
  const response = await fetch(url, { ...init, redirect: 'error', signal: AbortSignal.timeout(10000) });
  if (!response.ok) throw Error(`Keycloak respondió HTTP ${response.status}; revisa configuración, sesión y PKCE`);
  return response.json();
}
async function discovery() {
  const config = await jsonFetch(`${issuer}/.well-known/openid-configuration`);
  if (config.issuer !== issuer) throw Error('Discovery anuncia issuer inesperado');
  return config;
}
function redirect(res, url) { res.writeHead(303, { Location: url }); res.end(); }
function page(res, session, status = 200) {
  const button = (action, label) => `<form method="post" action="/${action}"><input type="hidden" name="csrf" value="${session.csrf}"><button>${label}</button></form>`;
  const report = session.report ? `<h2>Resultado verificado</h2><pre>${escape(JSON.stringify(session.report, null, 2))}</pre>` : '<p>Todavía no hay una autenticación real verificada en esta sesión.</p>';
  res.writeHead(status, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(`<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Banco Cloud · Fase 5</title><style>body{font:17px system-ui;max-width:850px;margin:48px auto;padding:0 22px;background:#f4f6fa;color:#162840}h1{font-size:30px}button{padding:12px 20px;background:#16489a;color:white;border:0;border-radius:6px;cursor:pointer}form{display:inline-block;margin:8px 10px 8px 0}pre{background:white;padding:20px;white-space:pre-wrap;overflow-wrap:anywhere;border:1px solid #ccd4df}.error{color:#a01d24}</style><h1>Banco Cloud · Prueba OIDC</h1><p>Fase 5. Herramienta local de diagnóstico; no es Angular ni el BFF definitivo.</p><p>Ingresa como <b>cliente.banco</b> o <b>admin.banco</b>. La contraseña se introduce exclusivamente en Keycloak.</p>${button('login','Iniciar sesión en Keycloak')}${session.refresh ? button('refresh','Probar renovación') : ''}${button('logout','Cerrar sesión en Keycloak')}${session.error ? `<p class="error">${escape(session.error)}</p>` : ''}${report}${businessPanel(session)}<p>Los tokens se conservan únicamente en memoria del proceso. Esta página muestra claims seleccionados después de verificar la firma, no el JWT completo. Los resultados corresponden al momento indicado.</p><p>El panel envía las operaciones al BFF, que valida el token y llama a los microservicios. API Gateway y Angular siguen pendientes.</p></html>`);
}
async function acceptTokens(tokens, config, session, nonce) {
  if (String(tokens.token_type).toLowerCase() !== 'bearer') throw Error('Tipo de token inesperado');
  const { keys } = await jsonFetch(config.jwks_uri);
  const claims = validateJwt(tokens.access_token, keys, {issuer, audience, access:true});
  if (claims.azp !== client) throw Error('Cliente autorizado inesperado');
  if (nonce) {
    const id = validateJwt(tokens.id_token, keys, {issuer, audience:client, nonce});
    if (id.sub !== claims.sub || (id.azp && id.azp !== client)) throw Error('Identidad OIDC inconsistente');
  }
  if (session.sub && !nonce && session.sub !== claims.sub) throw Error('Cambió la identidad al renovar');
  const roles = claims.resource_access?.[audience]?.roles ?? [];
  const scopes = (claims.scope ?? '').split(' ');
  if (!scopes.includes('banco-cloud-access')) throw Error('Falta scope banco-cloud-access');
  if (!Array.isArray(roles) || roles.includes('ADMIN') === roles.includes('CLIENTE')) throw Error('Para esta prueba se espera exactamente uno de los roles ADMIN/CLIENTE');
  session.access = tokens.access_token;
  session.sub = claims.sub;
  session.refresh = tokens.refresh_token;
  session.report = { usuario: claims.preferred_username, rol: roles, issuer: claims.iss, audience: claims.aud, scope: claims.scope, firmaRS256:'VALIDADA con JWKS', vigencia:'VALIDADA', expira: new Date(claims.exp * 1000).toISOString(), pkce: 'Intercambio realizado con S256', nonce: 'Validado durante login en ID token', operacion: nonce ? 'Login real' : 'Renovación real', verificadoEn: new Date().toISOString() };
  console.log(`Verificación satisfactoria: ${nonce ? 'login' : 'refresh'}; roles=${roles.filter(r => ['ADMIN','CLIENTE'].includes(r)).join(',')}`);
}

const server = http.createServer(async (req, res) => {
  res.setHeader('Cache-Control','no-store');
  // no-referrer convierte Origin en null en formularios POST reales de Chrome.
  // strict-origin conserva Origin y evita filtrar rutas/códigos en Referer.
  res.setHeader('Referrer-Policy','strict-origin');
  res.setHeader('X-Content-Type-Options','nosniff');
  res.setHeader('Content-Security-Policy',"default-src 'none'; style-src 'unsafe-inline'; form-action 'self' http://localhost:8080; frame-ancestors 'none'; base-uri 'none'");
  if (req.headers.host !== 'localhost:4200') { res.writeHead(400); return res.end('Usa http://localhost:4200/'); }
  const url = new URL(req.url, origin);
  const cookies = (req.headers.cookie ?? '').split(';').map(x => x.trim());
  let sid = cookies.find(x => x.startsWith('fase5='))?.slice(6);
  let session = sessions.get(sid);
  if (session && session.expires < Date.now()) { sessions.delete(sid); session = undefined; }
  if (!session) {
    if (sessions.size >= 100) { res.writeHead(503); return res.end('Demasiadas sesiones locales'); }
    sid = random(); session = { csrf:random(), expires:Date.now()+1800000 }; sessions.set(sid,session);
    res.setHeader('Set-Cookie',`fase5=${sid}; HttpOnly; SameSite=Lax; Path=/; Max-Age=1800`);
  }
  try {
    if (req.method === 'GET' && url.pathname === '/' && (url.searchParams.has('code') || url.searchParams.has('error'))) {
      const pending = session.pending; delete session.pending;
      if (!pending || Date.now() > pending.expires || url.searchParams.get('state') !== pending.state) throw Error('State ausente, vencido o incorrecto. Inicia nuevamente desde esta página.');
      if (url.searchParams.has('error')) throw Error('Login cancelado o rechazado por Keycloak');
      if (url.searchParams.has('iss') && url.searchParams.get('iss') !== issuer) throw Error('Issuer incorrecto en callback');
      const config = await discovery();
      const tokens = await jsonFetch(config.token_endpoint, {method:'POST', body:new URLSearchParams({grant_type:'authorization_code',client_id:client,redirect_uri:origin+'/',code:url.searchParams.get('code'),code_verifier:pending.verifier})});
      await acceptTokens(tokens, config, session, pending.nonce);
      session.error = null; return redirect(res,'/');
    }
    if (req.method === 'GET' && url.pathname === '/') return page(res,session);
    if (req.method !== 'POST' || !['/login','/refresh','/logout','/negocio'].includes(url.pathname)) { res.writeHead(404); return res.end('No encontrado'); }
    if (req.headers.origin !== origin) throw Error('Origen de formulario no permitido');
    let body = ''; for await (const chunk of req) { body += chunk; if (body.length > 2048) throw Error('Formulario demasiado grande'); }
    if (new URLSearchParams(body).get('csrf') !== session.csrf) throw Error('Formulario inválido');
    session.error = null;
    if (url.pathname === '/negocio') {
      try {
        session.business = await executeAction(new URLSearchParams(body).get('accion'), new URLSearchParams(body), session);
      } catch (error) {
        session.business = { error: error instanceof Error ? error.message : 'No se pudo consultar la API', comprobadoEn:new Date().toISOString() };
      }
      return redirect(res,'/');
    }
    const config = await discovery();
    if (url.pathname === '/login') {
      delete session.report; delete session.refresh; delete session.sub; delete session.access; delete session.business;
      const pending = {state:random(),nonce:random(),verifier:random(),expires:Date.now()+300000}; session.pending = pending;
      const login = new URL(config.authorization_endpoint);
      if (!login.href.startsWith(issuer+'/')) throw Error('Endpoint no confiable');
      login.search = new URLSearchParams({client_id:client,redirect_uri:origin+'/',response_type:'code',scope:'openid profile banco-cloud-access',state:pending.state,nonce:pending.nonce,code_challenge:createHash('sha256').update(pending.verifier).digest('base64url'),code_challenge_method:'S256',prompt:'login'}).toString();
      return redirect(res,login.href);
    }
    if (url.pathname === '/refresh') {
      if (!session.refresh) throw Error('Primero inicia sesión');
      const tokens = await jsonFetch(config.token_endpoint, {method:'POST', body:new URLSearchParams({grant_type:'refresh_token',client_id:client,refresh_token:session.refresh})});
      await acceptTokens(tokens,config,session); return redirect(res,'/');
    }
    const logout = new URL(config.end_session_endpoint);
    if (!logout.href.startsWith(issuer+'/')) throw Error('Endpoint no confiable');
    logout.search = new URLSearchParams({client_id:client,post_logout_redirect_uri:origin+'/'}).toString();
    sessions.delete(sid); res.setHeader('Set-Cookie','fase5=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0');
    return redirect(res,logout.href);
  } catch (error) {
    session.error = error instanceof Error ? error.message : 'Error de prueba';
    delete session.refresh; delete session.report; delete session.access; delete session.business;
    // Nunca registrar URLs de callback, códigos, tokens ni respuestas del token endpoint.
    if (url.search) return redirect(res,'/');
    return page(res,session,400);
  }
});
setInterval(() => { for (const [id,s] of sessions) if (s.expires < Date.now()) sessions.delete(id); },60000).unref();
server.on('error', error => { console.error(error.code === 'EADDRINUSE' ? 'Puerto 4200 ocupado; no se detuvo ningún proceso existente.' : 'No se pudo iniciar la herramienta local.'); process.exitCode=1; });
server.listen(4200,'127.0.0.1', () => console.log('Fase 5 lista: http://localhost:4200/ (Ctrl+C para detener)'));
