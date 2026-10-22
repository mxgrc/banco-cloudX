# Matriz de aceptación — pendiente de implementación

Esta tabla desglosa los dos indicadores oficiales en comprobaciones técnicas;
no añade indicadores ni ponderaciones a la pauta.

| Indicador | Criterio para máximo desempeño adaptado a Keycloak | Evidencia prevista |
| --- | --- | --- |
| Angular, 60% | Integración OIDC operativa, login y logout | Flujo real en navegador |
| Angular, 60% | Guards e interceptor funcionales | Rutas protegidas y llamada con access token |
| Angular, 60% | Obtención de tokens y consumo de API Gateway | Respuesta exitosa desde Angular |
| Angular, 60% | Lectura de roles y scopes desde claims | Vista y pruebas sin publicar tokens reales |
| BFF, 40% | Validación issuer, audience, firma y vigencia | Pruebas criptográficas positivas y negativas |
| BFF, 40% | Autorización por rol y errores adecuados | ADMIN autorizado, CLIENTE recibe 403, sin token 401 |

También se comprobará renovación de acceso, contemplada por la descripción de
fallas de niveles inferiores, y validación en API Gateway según el segundo indicador.

## Requisitos del encargo y caso

- [ ] Angular modular, vistas funcionales y compilación correcta.
- [ ] Microservicios Java/Spring Boot compilables y pruebas básicas.
- [ ] Entidades, repositorios y conexión a base cloud.
- [ ] Backend EC2 y consumo a través de API Gateway.
- [ ] Crear beneficio y asignarlo a categoría como ADMIN.
- [ ] Listar todos como ADMIN y disponibles como CLIENTE activo.
- [ ] Inscripción y consulta de beneficios propios.
- [ ] Aprobar registro y activar/desactivar acceso desde el portal.
- [ ] Repositorios GitHub limpios, README y entrega por AVA/correo.

## Pruebas de seguridad previstas

JWT válido; token ausente; firma alterada; expiración; issuer incorrecto;
audience incorrecta; rol insuficiente; usuario inactivo; lectura de datos ajenos;
ID token rechazado en API. Las pruebas de firma usarán verificación criptográfica
real: simular un usuario autenticado no demuestra validación de JWT.

Por etapa se documentarán cambios, verificaciones realizadas, pruebas manuales y
capturas sugeridas. No se marcará un criterio cumplido sin ejecución y evidencia.
