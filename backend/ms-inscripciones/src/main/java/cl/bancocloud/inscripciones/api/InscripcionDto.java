package cl.bancocloud.inscripciones.api;
import java.time.Instant;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;
import cl.bancocloud.inscripciones.domain.Inscripcion;
public record InscripcionDto(UUID id, UUID beneficioId, Instant fechaInscripcion) {
    public static InscripcionDto from(Inscripcion i) { return new InscripcionDto(i.getId(),i.getBeneficioId(),i.getFechaInscripcion()); }
    public record Crear(@NotNull UUID beneficioId) {}
}
