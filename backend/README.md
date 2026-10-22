# Banco Cloud — backend

Repositorio único para tres microservicios de negocio y un BFF:

| Componente | Responsabilidad |
| --- | --- |
| bff | Contrato HTTP para Angular, validación JWT y coordinación |
| ms-beneficios | Beneficios, categorías y disponibilidad para inscripción |
| ms-inscripciones | Inscripciones y beneficios propios del cliente |
| ms-usuarios | Perfil local, aprobación de registros y activación/desactivación |

## Estado y ejecución

Fase 6 en curso: tres módulos Java con entidades JPA, repositorios y reglas
de dominio. Los tres incorporan Service, DTO, Controller y seguridad JWT.
Beneficios e inscripciones consultan el estado del cliente por HTTP; inscripciones
también verifica disponibilidad. El BFF está implementado en :8084; API Gateway
sigue pendiente. Ver documentacion/fase7-bff.md para su arranque y pruebas.

Desde este directorio, en PowerShell: `.\mvnw.cmd -B -ntp verify`.
Alternativa con Maven instalado: `mvn.cmd -B -ntp verify`.
Los tests usan H2 en memoria y no necesitan Docker ni credenciales. Los JAR se
generan en target de cada módulo, excluido de Git. ms-usuarios puede iniciarse
con perfil local (H2 temporal) siguiendo documentacion/fase6-api-usuarios.md.
Sin ese perfil se exige DB_URL, DB_USERNAME, DB_PASSWORD y un esquema
PostgreSQL existente. Beneficios e inscripciones se ejecutan con las instrucciones
de documentacion/fase6-beneficios-inscripciones.md.
Spring Boot no carga automáticamente un .env: esas variables deben configurarse
en el entorno del proceso o en el futuro Compose.

Entorno comprobado el 2026-09-11: Java Temurin 17.0.18, Maven 3.9.14,
Git 2.49.0, Docker CLI 29.4.0 y Compose 5.1.1. El motor Docker no estaba
disponible inicialmente; posteriormente se inició correctamente (motor Linux).
La fase 3 incorpora Compose y generación local de credenciales en `keycloak/`.
Ver ese README para los comandos de arranque, parada y verificación.

Versiones fijadas: Java 17, Spring Boot 3.5.16 y Maven 3.9.14 con Wrapper oficial.
H2 tiene alcance runtime en los tres módulos para la demostración local;
PostgreSQL se incluye como driver runtime para la
posterior integración de base local/cloud.

## Seguridad

Keycloak gestiona identidad y contraseñas. Los servicios almacenan el identificador
`sub`, nunca la contraseña del cliente. API Gateway y BFF validarán el JWT de
forma independiente; los microservicios también protegerán sus operaciones.

Las variables y endpoints se documentarán junto a su implementación. Los archivos
`.env.example` futuros contendrán solo nombres y valores no sensibles. Los valores
reales serán locales. Un `.gitignore` no sustituye la revisión antes de publicar.

## Documentación

- `documentacion/arquitectura.md`: límites de componentes y decisiones.
- `documentacion/rubrica.md`: criterios y evidencias pendientes.
- `documentacion/entorno.md`: diagnóstico y restricciones del laboratorio.
- `documentacion/fase6-persistencia.md`: modelo, resultados y límites actuales.
- `documentacion/fase6-api-usuarios.md`: API de usuarios, seguridad y prueba local.
- `documentacion/fase6-beneficios-inscripciones.md`: nuevas rutas y comunicación.
- `documentacion/fase7-bff.md`: validación JWT independiente y paso por BFF.
- `keycloak/`: configuración local de identidad y herramienta de prueba OIDC.
- `infra/`: futura configuración de despliegue.

GitHub y AWS todavía no están configurados. Las pruebas locales no demuestran
integración RDS ni validación en API Gateway/BFF.
