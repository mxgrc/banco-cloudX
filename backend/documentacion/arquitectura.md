# Decisiones y alcance

## Fuentes

El PDF EP1_DSY1107_Estudiante_encargo.pdf exige Angular, varios microservicios
Spring Boot, EC2, API Gateway, base de datos cloud, validación JWT y entrega GitHub.
Su pauta tiene dos indicadores: autenticación Angular (60%) y validación BFF (40%).
El caso específico adjunto es Banco Cloud, aunque la plantilla menciona Pedidos360.

El estudiante confirma por correo docente la autorización para intentar Keycloak;
no se ha inspeccionado ese correo. Se adapta MSAL a equivalentes OIDC manteniendo
los criterios. RDS es la elección del proyecto para cumplir base de datos cloud.

## Componentes

Angular autentica con Keycloak mediante Authorization Code + PKCE. Envía el access
token a API Gateway, que valida y entrega la petición al BFF. Este vuelve a validar
y llama a ms-beneficios, ms-inscripciones y ms-usuarios. Cada servicio protege
sus operaciones y posee sus datos. El BFF no es un cuarto dominio de negocio.

Un repositorio contiene todo el backend; otro contiene Angular. El agregado de
ms-usuarios se fundamenta en la aprobación y activación de registros confirmadas
por el estudiante. No se agregan productos bancarios ajenos al caso.

## Identidad y permisos

Keycloak posee credenciales y roles; ms-usuarios posee el estado de acceso al portal.
Solo CLIENTE activo consulta disponibilidad, se inscribe y consulta sus inscripciones.
ADMIN crea beneficios, asigna categorías, lista todos y administra acceso de clientes.
No se presume que ADMIN herede automáticamente las operaciones CLIENTE.

Token ausente o inválido: 401. Token válido sin permiso o con acceso al portal
inactivo: 403 para las operaciones restringidas. El perfil pendiente debe tener
una ruta autenticada para consultar su estado. Los errores de infraestructura
no se confundirán con denegaciones de rol.

No bastará con ocultar botones. El backend verificará rol, estado y propiedad de
los datos. API Gateway verifica scopes configurados; no interpreta automáticamente
los roles anidados de Keycloak. Spring requiere conversión explícita de esos roles.

## Persistencia

Una instancia RDS puede alojar bases separadas para cada propietario, con usuarios
de conexión distintos. Keycloak requiere su propia base. No se comparten entidades
JPA ni se usan joins o claves foráneas entre bases de servicios.

La relación categoría-beneficio es interna a ms-beneficios. Las inscripciones
referencian beneficioId y usuarioSub lógicamente. Los beneficios disponibles para
inscripción y las inscripciones propias son consultas distintas.

## Pendientes antes de implementar cada área

- Fijar versiones compatibles y archivos lock/wrapper.
- Definir endpoints, DTO y reglas de disponibilidad/descuento.
- Concretar propagación de tokens y audiences entre servicios.
- Definir scopes y comprobar que no se acepte un ID token como access token.
- Concretar comprobación de estado en operaciones protegidas y errores de red.
- Definir HTTPS, integración privada/pública y protección contra bypass del gateway.
- Dimensionar recursos y estimar costos antes de AWS.

Estas decisiones se resolverán en su etapa; la estructura no demuestra todavía
el cumplimiento funcional de la evaluación.
