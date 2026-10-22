package cl.bancocloud.usuarios.api;

import java.util.List;
import org.springframework.data.domain.Page;
import cl.bancocloud.usuarios.domain.UsuarioPortal;

public record PaginaUsuarios(List<UsuarioResponse> contenido, int pagina, int tamano, long total) {
    public static PaginaUsuarios from(Page<UsuarioPortal> page) {
        return new PaginaUsuarios(page.map(UsuarioResponse::from).getContent(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
