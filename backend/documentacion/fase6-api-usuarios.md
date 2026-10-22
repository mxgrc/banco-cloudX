# Fase 6 — API de usuarios

El comando `./mvnw.cmd -B -ntp verify` utiliza el Wrapper de Maven, ejecuta sin
interacción (-B) y oculta progreso de transferencias (-ntp). Recorre las fases
hasta verify: compila, ejecuta los tests configurados y empaqueta los JAR.
No inicia permanentemente las APIs ni despliega AWS. No equivale a probar toda
la evaluación ni a ejecutar pruebas de integración que no estén configuradas.

## Implementación

Controller recibe HTTP y aplica roles; Service coordina transacciones y reglas;
Repository accede a datos; DTO devuelve solo el contrato público, no entidades.
El sub del cliente y el del administrador se obtienen del JWT, nunca del cuerpo.
Registrar de nuevo no reactiva cuentas. Un registro concurrente duplicado puede
responder 409; consultar el perfil antes de reintentar. Versionado optimista
protege cambios concurrentes de estado.

| Método y ruta | Rol | Resultado |
| --- | --- | --- |
| POST /api/usuarios/me | CLIENTE | Registro PENDIENTE o perfil existente, 200 |
| GET /api/usuarios/me | CLIENTE | Su propio estado, incluso pendiente/inactivo |
| GET /api/usuarios?estado=PENDIENTE&pagina=0&tamano=20 | ADMIN | Listado filtrable/paginado |
| PATCH /api/usuarios/{sub}/aprobar | ADMIN | PENDIENTE a ACTIVO |
| PATCH /api/usuarios/{sub}/desactivar | ADMIN | ACTIVO a INACTIVO |
| PATCH /api/usuarios/{sub}/reactivar | ADMIN | INACTIVO a ACTIVO |

Todas exigen scope banco-cloud-access. JWT: firma RS256, issuer, audiencia
banco-cloud-api, exp/nbf, sub y typ Bearer. Los roles se extraen exclusivamente
de resource_access.banco-cloud-api.roles. ADMIN no hereda CLIENTE.
Spring aplica su tolerancia temporal predeterminada de 60 segundos.
Sin token/token inválido: 401. Rol/scope insuficiente: 403. No registrado: 404.
Transición/conflicto de escritura: 409. Paginación/estado de filtro inválido: 400.

## Ejecución local

Desde banco-cloud-backend, después de verify, con Keycloak iniciado:

```powershell
java -jar .\ms-usuarios\target\ms-usuarios-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
```

Escucha solo en 127.0.0.1:8082. Mantener la terminal abierta; Ctrl+C lo detiene.
En otra terminal:

```powershell
curl.exe -i http://localhost:8082/api/usuarios/me
```

Se espera HTTP 401 porque no se envió token. No hay una página de login en 8082.
La prueba OIDC de 4200 sigue siendo independiente: todavía no llama esta API.
No pegar tokens ni contraseñas en capturas ni en el repositorio.

El perfil local usa H2 en memoria: los perfiles/aprobaciones desaparecen al
detener el proceso. No afecta las cuentas ni la base PostgreSQL de Keycloak.
Sin local, la configuración exige DB_URL, DB_USERNAME, DB_PASSWORD y esquema
PostgreSQL existente (ddl-auto=validate); migraciones y RDS aún pendientes.
KEYCLOAK_ISSUER y KEYCLOAK_JWKS permiten configurar el proveedor del despliegue.

## Pruebas y límites

UsuariosHttpTest usa MockMvc, firma RSA real y el mismo validador de producción.
Sustituye exclusivamente la obtención de clave pública: no prueba la descarga
JWKS ni un login real de Keycloak contra Spring. Tampoco prueba API Gateway/BFF.
Comprueba ciclo de estados, identidad derivada del token, separación de roles,
audiencia/issuer/firma/vigencia/tipo inválidos, scope faltante y errores HTTP.
Las suites JPA previas siguen cubriendo las tres bases aisladas de prueba.

Siguiente bloque: Services/DTO/Controllers de beneficios e inscripciones,
consulta del estado ACTIVO entre servicios y luego integración real BFF/Angular.
La desactivación aún no se aplica a operaciones de esos servicios no expuestas.

Referencia: [Spring Security Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
