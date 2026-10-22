package cl.bancocloud.usuarios.api;

import java.time.Instant;
import cl.bancocloud.usuarios.domain.*;

public record UsuarioResponse(String usuarioSub, EstadoUsuario estado, Instant fechaRegistro,
                              Instant fechaCambioEstado, String modificadoPorSub) {
    public static UsuarioResponse from(UsuarioPortal usuario) {
        return new UsuarioResponse(usuario.getUsuarioSub(), usuario.getEstado(), usuario.getFechaRegistro(),
                usuario.getFechaCambioEstado(), usuario.getModificadoPorSub());
    }
}
