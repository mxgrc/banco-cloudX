# Fase 6 — beneficios e inscripciones

Implementados Services transaccionales, DTO validados, Controllers y protección
JWT en ambos servicios. Cada módulo conserva sus tablas; no hay consultas SQL
ni claves foráneas entre servicios. El token original se propaga por HTTP.

## Rutas

| Servicio | Método y ruta | Acceso |
| --- | --- | --- |
| beneficios :8081 | POST /api/beneficios | ADMIN: nombre, descripcion, descuento |
| beneficios | GET /api/beneficios | ADMIN: todos, paginados |
| beneficios | POST /api/categorias | ADMIN: nombre |
| beneficios | GET /api/categorias | ADMIN: paginadas |
| beneficios | PUT /api/beneficios/{id}/categoria | ADMIN: categoriaId |
| beneficios | PATCH /api/beneficios/{id}/disponibilidad | ADMIN: disponible |
| beneficios | GET /api/beneficios/disponibles | CLIENTE ACTIVO: paginados |
| beneficios | GET /api/beneficios/{id}/disponibilidad | CLIENTE ACTIVO: verifica disponibilidad |
| inscripciones :8083 | POST /api/inscripciones | CLIENTE ACTIVO: beneficioId |
| inscripciones | GET /api/inscripciones/me | CLIENTE ACTIVO: solo propias, paginadas |

Listados: pagina=0 y tamano=20 predeterminados; máximo 100. Crear devuelve 201.
La identidad se obtiene del sub autenticado, sin aceptar usuarioSub del cuerpo.
Todas las rutas requieren scope banco-cloud-access y rol del cliente banco-cloud-api.
JWT: misma validación de firma RS256, issuer, audiencia y vigencia que usuarios.

Cada operación de cliente consulta GET /api/usuarios/me sin caché y exige ACTIVO.
Inscribir también consulta disponibilidad a beneficios antes de guardar.
401: token ausente/inválido; 403: rol/scope o estado insuficiente; 404: beneficio
o categoría inexistente; 409: duplicado/no disponible/conflicto; 503: dependencia
caída o respuesta inconsistente. Los clientes HTTP tienen límites de conexión de
2 segundos y lectura de 3 segundos. No registran tokens.

La elegibilidad corresponde al instante de la consulta remota: no es una
transacción distribuida. Una desactivación concurrente posterior a esa consulta
puede coincidir con la escritura en curso; las solicitudes siguientes vuelven a
comprobar el estado. La restricción SQL única protege inscripciones duplicadas.
Pausar un beneficio no elimina inscripciones existentes.

## Prueba local

Desde banco-cloud-backend, ejecutar verify y mantener Keycloak/usuarios iniciados.
En dos terminales distintas:

```powershell
java -jar .\ms-beneficios\target\ms-beneficios-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

```powershell
java -jar .\ms-inscripciones\target\ms-inscripciones-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

En otra terminal, ambas peticiones sin token deben devolver 401:

```powershell
curl.exe -i http://localhost:8081/api/beneficios
curl.exe -i http://localhost:8083/api/inscripciones/me
```

Los tres perfiles local usan H2 temporal independiente; detenerlos pierde datos
de negocio. Keycloak mantiene su base propia. Sin local se exige configuración
PostgreSQL y esquema existente. USUARIOS_URL/BENEFICIOS_URL configuran destinos
internos confiables; HTTPS y conectividad privada se prepararán para AWS.

## Alcance de verificación

Verify ejecutado el 2026-09-12: BUILD SUCCESS, 49 pruebas sin fallos ni errores
(18 beneficios, 19 usuarios, 12 inscripciones).

Tests HTTP con MockMvc: roles, validación, categoría, filtro, estados, duplicados
y separación de usuarios. Sustituyen JWT/servicios remotos en estas dos suites;
las pruebas de usuarios verifican firma RSA real. Tests de clientes HTTP usan
respuestas simuladas y comprueban propagación del Bearer, rechazo de estados,
respuesta vacía, sujeto incorrecto y error remoto. No representan todavía una
prueba completa con tokens reales de Keycloak a través de las tres APIs.

Siguiente paso: conectar la prueba autenticada/BFF con las APIs y comprobar el
ciclo cliente pendiente -> aprobación ADMIN -> inscripción -> desactivación.
Angular, BFF, Gateway y RDS siguen pendientes.

Referencia: [cliente HTTP de Spring](https://docs.spring.io/spring-framework/docs/6.2.9/javadoc-api/org/springframework/http/client/SimpleClientHttpRequestFactory.html).
