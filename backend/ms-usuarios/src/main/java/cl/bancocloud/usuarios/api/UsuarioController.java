package cl.bancocloud.usuarios.api;

import cl.bancocloud.usuarios.domain.EstadoUsuario;
import cl.bancocloud.usuarios.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public UsuarioResponse registrar(@AuthenticationPrincipal Jwt jwt) {
        return service.registrar(jwt.getSubject());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public UsuarioResponse consultar(@AuthenticationPrincipal Jwt jwt) {
        return service.consultar(jwt.getSubject());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PaginaUsuarios listar(@RequestParam(required = false) EstadoUsuario estado,
                                @RequestParam(defaultValue = "0") int pagina,
                                @RequestParam(defaultValue = "20") int tamano) {
        return service.listar(estado, pagina, tamano);
    }

    @PatchMapping("/{sub}/aprobar")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse aprobar(@PathVariable String sub, @AuthenticationPrincipal Jwt jwt) {
        return service.aprobar(sub, jwt.getSubject());
    }

    @PatchMapping("/{sub}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse desactivar(@PathVariable String sub, @AuthenticationPrincipal Jwt jwt) {
        return service.desactivar(sub, jwt.getSubject());
    }

    @PatchMapping("/{sub}/reactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse reactivar(@PathVariable String sub, @AuthenticationPrincipal Jwt jwt) {
        return service.reactivar(sub, jwt.getSubject());
    }
}
