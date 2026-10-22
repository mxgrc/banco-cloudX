package cl.bancocloud.inscripciones.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "inscripciones", uniqueConstraints = @UniqueConstraint(name = "uk_inscripcion_usuario_beneficio", columnNames = {"usuario_sub", "beneficio_id"}))
public class Inscripcion {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @NotBlank @Size(max = 255) @Column(name = "usuario_sub", nullable = false, length = 255, updatable = false)
    private String usuarioSub;
    @NotNull @Column(name = "beneficio_id", nullable = false, updatable = false)
    private UUID beneficioId;
    @NotNull @Column(nullable = false, updatable = false)
    private Instant fechaInscripcion;

    protected Inscripcion() { }

    public Inscripcion(String usuarioSub, UUID beneficioId, Instant fechaInscripcion) {
        if (usuarioSub == null || usuarioSub.isBlank() || usuarioSub.length() > 255)
            throw new IllegalArgumentException("Sub requerido, máximo 255 caracteres");
        this.usuarioSub = usuarioSub;
        this.beneficioId = Objects.requireNonNull(beneficioId, "Beneficio requerido");
        this.fechaInscripcion = Objects.requireNonNull(fechaInscripcion, "Fecha requerida");
    }
    public UUID getId() { return id; }
    public String getUsuarioSub() { return usuarioSub; }
    public UUID getBeneficioId() { return beneficioId; }
    public Instant getFechaInscripcion() { return fechaInscripcion; }
}
