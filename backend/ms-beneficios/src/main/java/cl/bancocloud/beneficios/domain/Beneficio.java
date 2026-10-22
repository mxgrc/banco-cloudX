package cl.bancocloud.beneficios.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "beneficios")
public class Beneficio {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Version private long version;
    @NotBlank @Size(max = 150) @Column(nullable = false, length = 150)
    private String nombre;
    @NotBlank @Size(max = 2000) @Column(nullable = false, length = 2000)
    private String descripcion;
    @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100")
    @Digits(integer = 3, fraction = 2) @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal descuento;
    @Column(nullable = false)
    private boolean disponible;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", foreignKey = @ForeignKey(name = "fk_beneficio_categoria"))
    private Categoria categoria;

    protected Beneficio() { }

    public Beneficio(String nombre, String descripcion, BigDecimal descuento) {
        this.nombre = texto(nombre, 150, "Nombre");
        this.descripcion = texto(descripcion, 2000, "Descripción");
        if (descuento == null || descuento.signum() <= 0 || descuento.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("Descuento debe ser mayor que 0 y hasta 100");
        try { this.descuento = descuento.setScale(2, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException("Descuento admite hasta dos decimales", ex); }
        this.disponible = true;
    }

    private static String texto(String valor, int maximo, String campo) {
        if (valor == null || valor.isBlank() || valor.strip().length() > maximo)
            throw new IllegalArgumentException(campo + " requerido, máximo " + maximo + " caracteres");
        return valor.strip();
    }

    public void asignarCategoria(Categoria categoria) { this.categoria = Objects.requireNonNull(categoria, "Categoría requerida"); }
    public void cambiarDisponibilidad(boolean disponible) { this.disponible = disponible; }
    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public BigDecimal getDescuento() { return descuento; }
    public boolean isDisponible() { return disponible; }
    public Categoria getCategoria() { return categoria; }
}
