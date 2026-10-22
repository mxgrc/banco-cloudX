package cl.bancocloud.beneficios.api;
import java.util.UUID;
import jakarta.validation.Valid;
import cl.bancocloud.beneficios.api.BeneficioDto.*;
import cl.bancocloud.beneficios.service.BeneficioService;
import cl.bancocloud.beneficios.integration.UsuarioAccess;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api")
public class BeneficioController {
    private final BeneficioService service;
    private final UsuarioAccess acceso;
    public BeneficioController(BeneficioService service, UsuarioAccess acceso) { this.service=service; this.acceso=acceso; }

    @GetMapping("/beneficios") @PreAuthorize("hasRole('ADMIN')")
    public Pagina<BeneficioDto> todos(@RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamano) {
        return service.listar(false,pagina,tamano);
    }
    @GetMapping("/beneficios/disponibles") @PreAuthorize("hasRole('CLIENTE')")
    public Pagina<BeneficioDto> disponibles(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamano) {
        acceso.exigirActivo(jwt.getSubject(),jwt.getTokenValue());
        return service.listar(true,pagina,tamano);
    }
    @GetMapping("/beneficios/{id}/disponibilidad") @PreAuthorize("hasRole('CLIENTE')")
    public BeneficioDto disponible(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        acceso.exigirActivo(jwt.getSubject(),jwt.getTokenValue());
        return service.disponible(id);
    }
    @PostMapping("/beneficios") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public BeneficioDto crear(@Valid @RequestBody Crear dto) { return service.crear(dto); }
    @PutMapping("/beneficios/{id}/categoria") @PreAuthorize("hasRole('ADMIN')")
    public BeneficioDto asignar(@PathVariable UUID id, @Valid @RequestBody CategoriaRequest dto) {
        return service.asignar(id,dto.categoriaId());
    }
    @PatchMapping("/beneficios/{id}/disponibilidad") @PreAuthorize("hasRole('ADMIN')")
    public BeneficioDto disponibilidad(@PathVariable UUID id, @Valid @RequestBody Disponibilidad dto) {
        return service.disponibilidad(id,dto.disponible());
    }
    @PostMapping("/categorias") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public CategoriaDto categoria(@Valid @RequestBody CrearCategoria dto) { return service.crearCategoria(dto); }
    @GetMapping("/categorias") @PreAuthorize("hasRole('ADMIN')")
    public Pagina<CategoriaDto> categorias(@RequestParam(defaultValue="0") int pagina, @RequestParam(defaultValue="20") int tamano) {
        return service.categorias(pagina,tamano);
    }
}
