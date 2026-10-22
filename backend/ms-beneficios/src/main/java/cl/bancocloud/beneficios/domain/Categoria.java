package cl.bancocloud.beneficios.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.UUID;

@Entity
@Table(name = "categorias", uniqueConstraints = @UniqueConstraint(name = "uk_categoria_nombre", columnNames = "nombre"))
public class Categoria {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @NotBlank @Size(max = 100) @Column(nullable = false, length = 100)
    private String nombre;

    protected Categoria() { }

    public Categoria(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 100)
            throw new IllegalArgumentException("Nombre de categoría requerido, máximo 100 caracteres");
        this.nombre = nombre.strip();
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
}
