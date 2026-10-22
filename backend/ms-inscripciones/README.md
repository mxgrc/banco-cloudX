# Microservicio de inscripciones

Base implementada: Inscripcion, repositorio JPA y 5 pruebas. Propietario de las
inscripciones. Pendiente capa de aplicación y validación de elegibilidad remota.

CLIENTE activo se inscribe en un beneficio disponible y consulta sus propias
inscripciones. La identidad se obtiene del `sub` del token validado, nunca de
un identificador de usuario libre enviado por el cliente.

Datos propuestos: id, beneficioId, usuarioSub y fechaInscripcion.
Se impedirá duplicar la inscripción de un usuario en el mismo beneficio.
La referencia al beneficio es lógica: no se crearán claves foráneas entre
bases de datos de microservicios. La disponibilidad se consultará al servicio dueño.
