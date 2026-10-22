# Fase 6 — primer bloque: dominio y persistencia

Implementado y verificado el 2026-09-12. La fase 6 completa todavía requiere
Services, DTO y Controllers; la exposición de rutas se coordinará con la fase 7.

## Modelo

| Servicio | Tablas propias | Relación |
| --- | --- | --- |
| ms-beneficios | categorias, beneficios | Una categoría tiene muchos beneficios; FK local categoria_id |
| ms-usuarios | usuarios_portal | Clave usuario_sub del Realm Keycloak configurado |
| ms-inscripciones | inscripciones | Referencias lógicas usuario_sub y beneficio_id, sin FK externa |

No se duplican contraseñas ni roles en usuarios_portal. El perfil se crea PENDIENTE;
aprobar lo activa, desactivar lo deja INACTIVO y reactivar lo vuelve ACTIVO.
Se guarda actor y fecha del último cambio, no un historial completo.
La entidad no verifica que el actor sea ADMIN: esa comprobación corresponde
a la capa de aplicación/seguridad pendiente, que usará el JWT autenticado.

## Decisiones adicionales al caso

- Descuento porcentual mayor que 0 y hasta 100, con dos decimales exactos usando
  BigDecimal. Estas reglas se proponen para implementar el campo ambiguo del caso.
- Disponibilidad booleana para nuevas inscripciones. Sin vencimientos, cupos ni
  cancelaciones inventadas. Inscripciones existentes no desaparecen al pausar un beneficio.
- Un beneficio puede crearse sin categoría y asignarse después: son operaciones
  separadas en el caso. La categoría tiene nombre único tras quitar espacios exteriores.
- Límites técnicos: categoría 100 caracteres, beneficio 150, descripción 2000;
  son decisiones del proyecto, no cifras de la rúbrica.
- Restricción SQL única para usuario_sub + beneficio_id. Impide duplicados aun
  cuando una operación omita una consulta previa desde Java.
- UUID locales; sub es cadena para no asumir formato UUID del proveedor.
- Versionado optimista en beneficios y usuarios. Manejo HTTP de conflictos pendiente.

## Límites de la prueba

Los repositorios reciben un sub en Java; al exponer HTTP debe derivarse del JWT,
no confiar en un identificador enviado por el navegador para consultar datos propios.
Antes de inscribir, la capa de aplicación deberá comprobar estado ACTIVO y
disponibilidad mediante los servicios dueños. Eso no está implementado todavía.

Cada suite utiliza H2 independiente con create-drop únicamente en test.
Fuera de test se exige PostgreSQL por variables de entorno y ddl-auto=validate.
No se conectó RDS ni se aplicaron migraciones. H2 no demuestra compatibilidad
completa con PostgreSQL: se añadirán pruebas sobre ese motor en la etapa de base.

## Resultado

`mvn.cmd -B -ntp verify` generó tres JAR y terminó BUILD SUCCESS.

| Suite | Tests | Evidencia |
| --- | --- | --- |
| BeneficiosPersistenceTest | 10 | Relación categoría, decimales, disponibilidad y validaciones |
| UsuariosPersistenceTest | 6 | Estados, transiciones, auditoría y consulta por estado |
| InscripcionesPersistenceTest | 5 | Persistencia, consulta por sujeto y unicidad |
| Total | 21 | Cero fallos, cero errores |

Los mensajes SQL 23505 de las pruebas de duplicados son esperados y comprobados
con aserciones; no son fallos pendientes del build. Reportes detallados en
target/surefire-reports de cada módulo, regenerables con verify.

## Prueba manual y evidencia

En PowerShell desde banco-cloud-backend: `.\mvnw.cmd -B -ntp verify`.
Guardar captura del Reactor Summary con los tres módulos SUCCESS y BUILD SUCCESS.
Este bloque todavía no tiene una URL de negocio para probar en navegador.

## Siguiente bloque

Services transaccionales, DTO y contratos HTTP; luego JWT, roles/scopes y pruebas
401/403. Completar elegibilidad entre servicios sin compartir tablas. El BFF no
se incluye aún en el reactor Maven porque su implementación es una etapa posterior.

Fuentes técnicas:
- https://docs.spring.io/spring-boot/3.5/maven-plugin/index.html
- https://maven.apache.org/tools/wrapper/index.html
