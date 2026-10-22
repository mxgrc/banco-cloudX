package cl.bancocloud.inscripciones.api;
import jakarta.validation.Valid;
import cl.bancocloud.inscripciones.integration.*;
import cl.bancocloud.inscripciones.service.InscripcionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@RestController
@RequestMapping("/api/inscripciones")
@PreAuthorize("hasRole('CLIENTE')")
public class InscripcionController {
    private final InscripcionService service;
    private final UsuarioAccess usuarios;
    private final BeneficioAccess beneficios;
    public InscripcionController(InscripcionService service,UsuarioAccess usuarios,BeneficioAccess beneficios) {
        this.service=service; this.usuarios=usuarios; this.beneficios=beneficios;
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public InscripcionDto crear(@Valid @RequestBody InscripcionDto.Crear dto,@AuthenticationPrincipal Jwt jwt) {
        // Consultas remotas antes de abrir la transacción local de escritura.
        usuarios.exigirActivo(jwt.getSubject(),jwt.getTokenValue());
        beneficios.exigirDisponible(dto.beneficioId(),jwt.getTokenValue());
        return service.crear(jwt.getSubject(),dto.beneficioId());
    }
    @GetMapping("/me")
    public Pagina<InscripcionDto> propias(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamano) {
        usuarios.exigirActivo(jwt.getSubject(),jwt.getTokenValue());
        return service.propias(jwt.getSubject(),pagina,tamano);
    }
}
