# BFF

Implementado en 127.0.0.1:8084. Desde banco-cloud-backend:

```powershell
java -jar .\bff\target\bff-0.1.0-SNAPSHOT.jar
```

No necesita base de datos ni perfil local. Ver ../documentacion/fase7-bff.md.

Será la API consumida por Angular a través de API Gateway. Ya valida el access
token (firma, issuer, audience y vigencia), aplica permisos y coordina
llamadas a los microservicios. No será propietario de tablas de negocio.

Debe conservar los códigos 401 y 403 de forma coherente. No aceptará como prueba
de identidad simples cabeceras de usuario proporcionadas por el navegador.
