# Fase 5 — prueba local OIDC

Herramienta de diagnóstico Node.js sin dependencias npm, exclusiva de desarrollo.
No sustituye Angular, Spring Security, el BFF ni API Gateway. Hace el intercambio
de código en Node para inspeccionar los tokens sin enviarlos a la página; Angular
hará posteriormente ese intercambio desde su adaptador OIDC en el navegador.

## Requisitos y ejecución

- Node 22 o superior; probado en Node 24.
- Keycloak local en http://localhost:8080 y realm banco-cloud configurado.
- Cliente público banco-cloud-frontend: Standard flow, PKCE S256, redirect URI
  y post logout URI http://localhost:4200/.
- Audience banco-cloud-api, scope banco-cloud-access, usuarios con roles separados.
- Puerto 4200 libre. La herramienta escucha solo en 127.0.0.1; usar localhost en la URL.

Desde este directorio:

```powershell
npm.cmd test
npm.cmd start
```

Abrir http://localhost:4200/ y pulsar Iniciar sesión. Introducir la contraseña
exclusivamente en la página de Keycloak. La herramienta no pide contraseñas.
Usar Ctrl+C en la terminal para detenerla antes de iniciar Angular en el mismo puerto.

## Verificaciones

Authorization Code con verifier aleatorio, challenge S256, state de un solo uso
vinculado a cookie HttpOnly/SameSite y nonce validado en ID token. Se validan firma
RS256 mediante JWKS del issuer fijo, aud, iss, exp, nbf si existe, iat y sub.
El ID token se valida para frontend; el access token para banco-cloud-api y typ
Bearer. Se comprueban azp, scope y exactamente un rol de negocio para esta prueba.
Este contrato estricto es para los dos usuarios de prueba, no una regla universal
que prohíba tener varios roles en otras aplicaciones.

Se muestran solo claims seleccionados después de validarlos, nunca JWT completos.
Los refresh tokens se mantienen en memoria (sesiones locales de 30 minutos) y
no se escriben en archivos. Los reportes son resultados del instante de validación,
no garantía de que el token continúe vigente después. No se publican APIs de negocio.

Logout redirige a Keycloak con client_id y post_logout_redirect_uri, sin tokens en
la URL; Keycloak puede pedir confirmación. No implica revocación instantánea de
todos los access tokens ya emitidos. Los tokens de esta herramienta se descartan.

## Prueba manual y evidencia

1. Login como cliente.banco: reporte CLIENTE, sin ADMIN, audience API y scope.
2. Pulsar Probar renovación: debe mostrar Renovación real y nueva verificación.
3. Cerrar sesión y confirmar en Keycloak; volver sin reporte autenticado.
4. Login como admin.banco: reporte ADMIN, sin CLIENTE. Repetir renovación/logout.
5. Guardar capturas de reportes, sin contraseñas ni tokens completos.

Los tests de jwt.test.mjs usan claves RSA generadas en memoria: prueban tokens
válidos y rechazos por firma alterada, issuer, audience, expiración, iat, nbf,
algoritmo, kid, sub, nonce y uso de ID token como access token.
Estas pruebas no demuestran todavía 401/403 en Spring o API Gateway.

Pendiente de ejecución manual: login con los dos usuarios y contraseña privada,
renovación y logout. Evaluate de la consola de Keycloak no equivale a esta prueba.

## Resultado automático — 2026-09-12

14 tests del validador aprobados. `node smoke.mjs` comprobó página HTTP 200,
cabecera no-store, rechazo de origen ajeno, redirección con PKCE S256 y nonce,
pantalla de contraseña de Keycloak HTTP 200 y rechazo de callback con state
incorrecto. Esa prueba no introduce credenciales ni obtiene tokens de usuarios.

Referencias:
- https://www.keycloak.org/securing-apps/oidc-layers
- https://nodejs.org/api/crypto.html

## Corrección de formularios Chrome — 2026-09-12

Se reprodujo en un navegador aislado el rechazo del login: Referrer-Policy
no-referrer ocasionaba Origin: null en formularios POST. La prueba HTTP inicial
inyectaba Origin manualmente y no reproducía ese comportamiento del navegador.
Se cambió la política a strict-origin, que conserva el origen sin transmitir rutas
ni parámetros en Referer. Se conservan el control exacto de Origin y el token CSRF.

browser-smoke.mjs comprueba un clic real hasta el formulario de Keycloak, sin
credenciales. Requiere Playwright disponible externamente y Chrome instalado:
`node browser-smoke.mjs <ruta-al-paquete-playwright>`.
No utiliza el perfil personal del navegador. La opción --expect-blocked sirve
solo para reproducir la versión anterior con no-referrer.
