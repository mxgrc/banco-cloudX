package cl.bancocloud.beneficios.api;
import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.*;
import cl.bancocloud.beneficios.domain.Beneficio;

public record BeneficioDto(UUID id, String nombre, String descripcion, BigDecimal descuento,
                           boolean disponible, UUID categoriaId) {
    public static BeneficioDto from(Beneficio b) {
        return new BeneficioDto(b.getId(), b.getNombre(), b.getDescripcion(), b.getDescuento(),
                b.isDisponible(), b.getCategoria() == null ? null : b.getCategoria().getId());
    }
    public record Crear(@NotBlank @Size(max=150) String nombre, @NotBlank @Size(max=2000) String descripcion,
                        @NotNull @DecimalMin(value="0",inclusive=false) @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal descuento) {}
    public record CategoriaRequest(@NotNull UUID categoriaId) {}
    public record Disponibilidad(@NotNull Boolean disponible) {}
    public record CrearCategoria(@NotBlank @Size(max=100) String nombre) {}
    public record CategoriaDto(UUID id, String nombre) {}
}
