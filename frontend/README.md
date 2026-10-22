# Banco Cloud — frontend

Aplicación Angular para Gestión de Beneficios y Descuentos.

## Estado

Fase 8: aplicación Angular implementada con Angular 21.2 y Node 24.11.0.
`npm run build` aprobado. `npm start` sirve el portal en localhost:4200.

## Funcionalidad prevista

- Login/logout Keycloak mediante Authorization Code + PKCE S256.
- Estado de autenticación, Auth Guard, Role Guard e interceptor HTTP.
- Access token enviado únicamente a destinos API autorizados.
- Cliente: estado de aprobación, beneficios disponibles, inscripción y beneficios propios.
- Administrador: creación de beneficios, categorías, listado y aprobación/activación de clientes.
- Manejo de 401, 403 y errores de conectividad.

La autorización efectiva se mantiene en backend. El navegador no contiene
client secret ni credenciales AWS. La configuración pública de issuer, client ID
y URL de API se documentará al integrar Keycloak. No se pegarán JWT en el código.

## Entrega

Se versionarán código fuente, configuración reproducible y lockfile de dependencias.
Se excluyen node_modules, compilaciones, cachés y secretos. GitHub no está todavía
configurado y no se ha publicado este repositorio.

La documentación común está en el repositorio hermano banco-cloud-backend,
directorio documentacion. Para desarrollo, `proxy.conf.json` dirige `/api` al BFF
en `http://127.0.0.1:8084`. El despliegue se definirá junto con HTTPS.

## Ejecución local

```powershell
npm install
npm start
```

Abre http://localhost:4200. La SPA redirige a Keycloak con Authorization Code
PKCE S256. En cliente muestra estado, beneficios e inscripciones; en administrador
ofrece beneficios, categorías y aprobación/desactivación/reactivación de clientes.
El interceptor renueva el token cuando quedan 30 segundos y solo añade Bearer a
rutas `/api` del mismo origen. Las respuestas 401, 403, 409 y fallos de conexión
se muestran con mensajes comprensibles.

## Versiones y verificación

Angular 21.2 es compatible con Node 24 y TypeScript 5.9 según la tabla oficial de
versiones de Angular. Keycloak JS 26.2 usa `pkceMethod: 'S256'`.
`npm run build` genera `dist/banco-cloud` (326.50 kB iniciales en esta ejecución).
Los tokens no se escriben en localStorage ni en el código fuente; los gestiona el
adaptador en memoria. CORS y URLs HTTPS de producción quedan para AWS.
