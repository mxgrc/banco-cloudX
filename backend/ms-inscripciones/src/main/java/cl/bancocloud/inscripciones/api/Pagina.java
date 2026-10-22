package cl.bancocloud.inscripciones.api;
import java.util.List;
import org.springframework.data.domain.*;
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long total) {
    public static <T> Pagina<T> from(Page<T> page) {
        return new Pagina<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
    public static Pageable request(int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 100) throw new IllegalArgumentException("Página >= 0 y tamaño entre 1 y 100");
        return PageRequest.of(pagina, tamano, Sort.by("id"));
    }
}
