# Fase 7 — BFF local

Implementado como cuarto módulo Maven, sin JPA ni base propia.
Expone las 16 rutas explícitas /api de usuarios, categorías, beneficios e
inscripciones. No acepta URLs de destino ni cabeceras de identidad del navegador.

## Seguridad y comunicación

Valida firma RS256 mediante JWKS, issuer, audiencia banco-cloud-api, exp/nbf
(tolerancia Spring de 60 segundos), sub y typ=Bearer. Exige scope banco-cloud-access
y roles de resource_access.banco-cloud-api.roles. ADMIN no hereda CLIENTE.
Un token sin audiencia o expiración se rechaza.

Propaga el mismo Bearer al servicio dueño, que vuelve a validarlo. No propaga
cookies ni cabeceras arbitrarias. Los servicios mantienen su comprobación ACTIVO
sin caché; aplica la limitación de concurrencia documentada en fase 6.

USUARIOS_URL, BENEFICIOS_URL, INSCRIPCIONES_URL configuran destinos confiables
(defaults localhost 8082, 8081, 8083). Apache HttpClient permite PATCH, limita
conexión a 2 segundos y lectura a 10 segundos. No sigue redirecciones ni
reintenta automáticamente escrituras. Respuesta máxima de 1 MiB.

Conserva códigos 2xx/4xx, incluidos 401, 403 y 409. Error 5xx del servicio,
redirección o respuesta no JSON: 502. Error de conexión/timeout: 503.
El sub de las rutas administrativas acepta alfanuméricos, guion y guion bajo,
hasta 255 caracteres; compatible con los UUID actuales de Keycloak.

## Verificación

El 2026-09-12, mvnw.cmd -B -ntp -pl bff -am verify terminó BUILD SUCCESS:
59 tests sin fallos (46 seguridad/controladores y 13 comunicación).
Las 16 rutas se prueban con el rol correcto e incorrecto. Se prueban también
firma incorrecta, claims inválidos/ausentes, scope, rol de otro cliente,
validación DTO y propagación del token.

La suite usa firma RSA real con clave de prueba y validadores de producción;
sustituye la obtención de JWKS y el servicio remoto. Las pruebas de comunicación
simulan HTTP y comprueban códigos, PATCH, cabeceras, JSON, redirecciones y fallos.
El panel Node pasa 20 tests. Falta la prueba manual con Keycloak real vía BFF.

## Prueba manual

Mantener Keycloak y los tres servicios iniciados. Desde banco-cloud-backend:

```powershell
java -jar .\bff\target\bff-0.1.0-SNAPSHOT.jar
```

Mantener la terminal abierta. En otra:

```powershell
curl.exe -i http://localhost:8084/api/usuarios/me
```

Esperado 401. Reiniciar solo Node del puerto 4200 (Ctrl+C en su terminal y luego
node .\keycloak\fase5\server.mjs). Iniciar sesión nuevamente en localhost:4200.
El panel dice Prueba a través del BFF · Fase 7; las respuestas incluyen via:
BFF :8084. Todas las acciones del panel pasan por ese destino.

1. CLIENTE activo: Consultar mi estado y Ver mis inscripciones deben dar 200.
2. ADMIN: Listar clientes y desactivar el sub deben dar 200.
3. CLIENTE: Ver mis inscripciones debe dar 403; reactivar desde ADMIN y repetir
   debe recuperar 200. Usar navegadores independientes para conservar sesiones.
4. Inscripción nueva: 201; repetida: 409. Puede usarse un beneficio previo.

Si aparece 401, renovar o iniciar sesión. Reiniciar Node elimina sus sesiones,
pero no los datos de los servicios. Mantener Java abierto para conservar H2 local.

Pendientes: Angular, CORS según despliegue, validación API Gateway independiente,
base PostgreSQL/RDS persistente, HTTPS y AWS. BFF local no completa la evaluación.

Referencias: [Spring Security JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
y [cliente HTTP Spring](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/http/client/HttpComponentsClientHttpRequestFactory.html).
