# Infraestructura

Despliegue pendiente. Región propuesta: us-east-1.

Servicios previstos: EC2, API Gateway HTTP API con JWT Authorizer, RDS PostgreSQL,
VPC y Security Groups. Usar LabRole/LabInstanceProfile cuando corresponda.

La ruta de integración API Gateway → BFF (pública HTTPS o privada con VPC Link
y balanceador) se decidirá antes del despliegue con costos y permisos comprobados.
No se considera cerrada la protección contra acceso directo al BFF.

Keycloak requiere una dirección HTTPS estable. La IP de EC2 puede cambiar entre
sesiones; se evaluará DNS actualizable o IP estable con su costo y disponibilidad.
No se crearán recursos cloud durante la preparación local.
