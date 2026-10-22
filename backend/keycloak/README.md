# Keycloak

Fase 3: Docker Compose con Keycloak 26.7.3 y PostgreSQL 17.6 persistente.
Fase 4: Realm banco-cloud, cliente público Angular con PKCE S256, audience API,
roles CLIENTE/ADMIN y usuarios de prueba. La configuración exportable debe
sanitizarse antes de incorporarla a Git.

No se incluirán contraseñas reales, tokens, claves privadas ni volúmenes de datos.
El despliegue cloud requerirá HTTPS y un issuer estable; la dirección local de
desarrollo no será el issuer utilizado por API Gateway.

## Ejecutar localmente

Desde este directorio en PowerShell, con Docker Desktop y motor Linux activos:

```powershell
./initialize-local.ps1
docker compose config --quiet
docker compose up -d
docker compose ps
```

Abrir http://localhost:8080/admin/ e ingresar como bootstrap-admin usando
KC_BOOTSTRAP_ADMIN_PASSWORD del archivo local .env. El script genera contraseñas
aleatorias y conserva .env si ya existe. No compartir ese archivo ni fotografiarlo.
El administrador de arranque es temporal; en fase 4 se creará uno permanente.
Este usuario administra Keycloak y no representa al ADMIN de negocio Banco Cloud.

Solo se publica 127.0.0.1:8080; PostgreSQL no publica puertos al host.
start-dev y HTTP son exclusivamente para desarrollo local, no para AWS.

## Detener y reanudar

```powershell
docker compose stop
docker compose start
```

El volumen conserva usuarios y realms. `docker compose down` también conserva
el volumen; no agregar `-v` o `--volumes`, que borraría los datos.
Modificar .env no cambia las contraseñas que ya se inicializaron en la base.

## Verificación y evidencia

Verificado el 2026-09-11 (hora de Chile): PostgreSQL healthy y conexión SQL;
consola HTTP 200; discovery con issuer correcto; JWKS con clave RSA de firma.
Se reiniciaron PostgreSQL y Keycloak y se comprobó que el id del realm master
se conservó y los endpoints siguieron respondiendo. Login por navegador pendiente
de comprobación manual. La primera descarga falló con EOF y el reintento funcionó.

Para repetir la prueba HTTP: `./verify-local.ps1`.

```powershell
docker compose logs --tail 60 keycloak
Invoke-RestMethod http://localhost:8080/realms/master/.well-known/openid-configuration | Select-Object issuer, jwks_uri
```

El issuer esperado es http://localhost:8080/realms/master. El Realm banco-cloud
y la integración Angular están pendientes de sus etapas correspondientes.
Guardar capturas de los dos contenedores activos y de la consola administrativa
después del login, sin credenciales visibles.

Fuentes: https://www.keycloak.org/getting-started/getting-started-docker y
https://www.keycloak.org/server/containers.
