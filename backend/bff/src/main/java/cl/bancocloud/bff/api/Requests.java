package cl.bancocloud.bff.api;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;
public final class Requests {
    private Requests() {}
    public record Beneficio(@NotBlank @Size(max=150) String nombre,@NotBlank @Size(max=2000) String descripcion,
            @NotNull @DecimalMin(value="0",inclusive=false) @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal descuento) {}
    public record Categoria(@NotBlank @Size(max=100) String nombre) {}
    public record AsignarCategoria(@NotNull UUID categoriaId) {}
    public record Disponibilidad(@NotNull Boolean disponible) {}
    public record Inscripcion(@NotNull UUID beneficioId) {}
}
