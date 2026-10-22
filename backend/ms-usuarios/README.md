# Microservicio de usuarios

Implementado: dominio, repositorio, Service transaccional, DTO, Controller y JWT.
Propietario del perfil de acceso al portal. Comandos y rutas en
../documentacion/fase6-api-usuarios.md. Perfil local H2 temporal, no RDS.

Datos propuestos: usuarioSub, estado, fechaRegistro y datos de auditoría de
aprobación/cambio de estado. Estados: PENDIENTE, ACTIVO e INACTIVO.

POST /api/usuarios/me registra al CLIENTE autenticado como pendiente sin alterar
un registro existente. El frontend futuro deberá invocar esta operación.
ADMIN puede aprobar, desactivar y reactivar mediante rutas protegidas.
El cliente pendiente o inactivo solo podrá consultar su estado entre las rutas
de cliente; las funciones de beneficios e inscripción exigirán estado ACTIVO.

Esta desactivación afecta al portal, no al login global de Keycloak. El estado
se comprobará en backend para bloquear operaciones posteriores aunque el JWT
no haya expirado. Un fallo al consultar el estado no autorizará operaciones.
La cuenta ADMIN inicial se preparará de forma controlada, sin registro público
con privilegios administrativos.
