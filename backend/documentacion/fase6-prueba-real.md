# Prueba manual Keycloak y las tres APIs

El estudiante confirmó mediante capturas registro, aprobación, inscripción,
consulta, rechazo de duplicado 409, bloqueo por desactivación 403 y recuperación
200 conservando inscripciones. Corresponde al acceso directo previo al BFF.
Desde fase 7 el panel envía las acciones a :8084: debe iniciarse el BFF para
repetir esta prueba; ver fase7-bff.md.

Mantener Keycloak y los tres microservicios con perfil local abiertos. Reiniciar
solo la herramienta Node del puerto 4200 (Ctrl+C en su terminal), y desde
banco-cloud-backend ejecutar `node .\keycloak\fase5\server.mjs`.
Abrir http://localhost:4200 e iniciar sesión de nuevo. El acceso anterior estaba
en memoria del proceso que se detuvo.

1. CLIENTE: pulsar Registrar mi perfil. Esperado 200 y PENDIENTE (o estado
   existente si ya se registró). Copiar usuarioSub para la prueba administrativa.
2. CLIENTE pendiente: Ver beneficios disponibles devuelve 403.
3. Cerrar sesión y entrar como admin.banco. Listar clientes y copiar usuarioSub
   del cliente, no el username cliente.banco.
4. Pegar ese sub y pulsar Aprobar cliente. Esperado 200 y ACTIVO. Crear beneficio
   de prueba (15%) devuelve 201. Cada clic crea un beneficio nuevo.
5. Cerrar sesión y entrar como cliente.banco. Ver beneficios disponibles devuelve
   200. Copiar id del beneficio, pegarlo en ID del beneficio y pulsar Inscribirme:
   esperado 201. Ver mis inscripciones devuelve 200 con la inscripción propia.
6. Repetir inscripción: 409 sin crear duplicado.
7. ADMIN: desactivar el mismo sub. CLIENTE: tras el próximo login o con sesión
   ya abierta en otro navegador, Ver beneficios disponibles o Ver mis
   inscripciones debe responder 403. Reactivar devuelve ACTIVO.

Si aparece 401, usar Probar renovación o iniciar sesión de nuevo y repetir.
Si aparece error de conexión/503, comprobar las tres terminales de Java.
Los perfiles local son temporales: al reiniciar un microservicio se pierden
sus datos. Keycloak conserva las identidades en su base independiente.

La herramienta mantiene access/refresh tokens solo en memoria, usa formularios
con CSRF y origen exacto, y envía Bearer únicamente a destinos locales fijos.
La API vuelve a validar el token y los permisos. El panel no es el BFF final.

Verificación automática del panel: `node --test` desde keycloak/fase5, 20 tests
aprobados (14 JWT y 6 panel). El recorrido manual anterior queda pendiente de
ejecución por el estudiante; no se simula un login real ni se solicitan claves.
Guardar capturas de respuestas y roles, nunca de tokens o contraseñas.
