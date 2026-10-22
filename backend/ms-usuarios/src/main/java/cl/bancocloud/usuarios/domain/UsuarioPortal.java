package cl.bancocloud.usuarios.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "usuarios_portal")
public class UsuarioPortal {
    // Identificador OIDC del realm configurado; no es un nombre ni un correo.
    @Id @NotBlank @Size(max = 255) @Column(name = "usuario_sub", length = 255)
    private String usuarioSub;
    @Version private long version;
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private EstadoUsuario estado;
    @NotNull @Column(nullable = false, updatable = false)
    private Instant fechaRegistro;
    private Instant fechaCambioEstado;
    @Column(length = 255) private String modificadoPorSub;

    protected UsuarioPortal() { }

    public UsuarioPortal(String usuarioSub, Instant fechaRegistro) {
        this.usuarioSub = sujeto(usuarioSub);
        this.fechaRegistro = Objects.requireNonNull(fechaRegistro, "Fecha requerida");
        this.estado = EstadoUsuario.PENDIENTE;
    }

    public void aprobar(String administradorSub, Instant fecha) {
        if (estado != EstadoUsuario.PENDIENTE) throw new IllegalStateException("Solo se aprueba un registro pendiente");
        cambiar(EstadoUsuario.ACTIVO, administradorSub, fecha);
    }
    public void desactivar(String administradorSub, Instant fecha) {
        if (estado != EstadoUsuario.ACTIVO) throw new IllegalStateException("Solo se desactiva un usuario activo");
        cambiar(EstadoUsuario.INACTIVO, administradorSub, fecha);
    }
    public void reactivar(String administradorSub, Instant fecha) {
        if (estado != EstadoUsuario.INACTIVO) throw new IllegalStateException("Solo se reactiva un usuario inactivo");
        cambiar(EstadoUsuario.ACTIVO, administradorSub, fecha);
    }
    private void cambiar(EstadoUsuario nuevo, String administradorSub, Instant fecha) {
        String actor = sujeto(administradorSub);
        Objects.requireNonNull(fecha, "Fecha requerida");
        Instant ultima = fechaCambioEstado == null ? fechaRegistro : fechaCambioEstado;
        if (fecha.isBefore(ultima)) throw new IllegalArgumentException("Fecha de cambio anterior al estado actual");
        estado = nuevo;
        fechaCambioEstado = fecha;
        modificadoPorSub = actor;
    }
    private static String sujeto(String sub) {
        if (sub == null || sub.isBlank() || sub.length() > 255) throw new IllegalArgumentException("Sub requerido, máximo 255 caracteres");
        return sub;
    }
    public String getUsuarioSub() { return usuarioSub; }
    public EstadoUsuario getEstado() { return estado; }
    public Instant getFechaRegistro() { return fechaRegistro; }
    public Instant getFechaCambioEstado() { return fechaCambioEstado; }
    public String getModificadoPorSub() { return modificadoPorSub; }
    public boolean isActivo() { return estado == EstadoUsuario.ACTIVO; }
}
