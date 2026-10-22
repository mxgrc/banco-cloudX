package cl.bancocloud.bff.api;

import java.util.*;
import jakarta.validation.Valid;
import com.fasterxml.jackson.databind.JsonNode;
import cl.bancocloud.bff.integration.BusinessGateway;
import static cl.bancocloud.bff.integration.BusinessGateway.Destination.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PortalController {
    private final BusinessGateway gateway;
    public PortalController(BusinessGateway gateway) { this.gateway=gateway; }

    @PostMapping("/usuarios/me") @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<JsonNode> registrar(@AuthenticationPrincipal Jwt jwt) {
        return call(USUARIOS,HttpMethod.POST,"/api/usuarios/me",Map.of(),null,jwt);
    }
    @GetMapping("/usuarios/me") @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<JsonNode> perfil(@AuthenticationPrincipal Jwt jwt) {
        return call(USUARIOS,HttpMethod.GET,"/api/usuarios/me",Map.of(),null,jwt);
    }
    @GetMapping("/usuarios") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JsonNode> usuarios(@RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamano,@RequestParam(required=false) String estado,@AuthenticationPrincipal Jwt jwt) {
        var query=new HashMap<String,Object>(page(pagina,tamano));
        if(estado!=null) {
            if(!List.of("PENDIENTE","ACTIVO","INACTIVO").contains(estado)) throw new IllegalArgumentException("Estado no válido");
            query.put("estado",estado);
        }
        return call(USUARIOS,HttpMethod.GET,"/api/usuarios",query,null,jwt);
    }
    @PatchMapping("/usuarios/{sub}/aprobar") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JsonNode> aprobar(@PathVariable String sub,@AuthenticationPrincipal Jwt jwt) {
        return state(sub,"aprobar",jwt);
    }
    @PatchMapping("/usuarios/{sub}/desactivar") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JsonNode> desactivar(@PathVariable String sub,@AuthenticationPrincipal Jwt jwt) {
        return state(sub,"desactivar",jwt);
    }
    @PatchMapping("/usuarios/{sub}/reactivar") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JsonNode> reactivar(@PathVariable String sub,@AuthenticationPrincipal Jwt jwt) {
        return state(sub,"reactivar",jwt);
    }
    @GetMapping("/beneficios") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JsonNode> beneficios(@RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamano,@AuthenticationPrincipal Jwt jwt) {
        return call(BENEFICIOS,HttpMethod.GET,"/api/beneficios",page(pagina,tamano),null,jwt);
    }
    @PostMapping("/beneficios") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JsonNode> crear(@Valid @RequestBody Requests.Beneficio dto,@AuthenticationPrincipal Jwt jwt) {
        return call(BENEFICIOS,HttpMethod.POST,"/api/beneficios",Map.of(),dto,jwt);
    }
    @GetMapping("/beneficios/disponibles") @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<JsonNode> disponibles(@RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamano,@AuthenticationPrincipal Jwt jwt) {
        return call(BENEFICIOS,HttpMethod.GET,"/api/beneficios/disponibles",page(pagina,tamano),null,jwt);
    }
    @GetMapping("/beneficios/{id}/disponibilidad") @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<JsonNode> disponible(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) {
        return call(BENEFICIOS,HttpMethod.GET,"/api/beneficios/"+id+"/disponibilidad",Map.of(),null,jwt);
    }
    @PutMapping("/beneficios/{id}/categoria") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JsonNode> categoria(@PathVariable UUID id,@Valid @RequestBody Requests.AsignarCategoria dto,@AuthenticationPrincipal Jwt jwt) {
        return call(BENEFICIOS,HttpMethod.PUT,"/api/beneficios/"+id+"/categoria",Map.of(),dto,jwt);
    }
    @PatchMapping("/beneficios/{id}/disponibilidad") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JsonNode> disponibilidad(@PathVariable UUID id,@Valid @RequestBody Requests.Disponibilidad dto,@AuthenticationPrincipal Jwt jwt) {
        return call(BENEFICIOS,HttpMethod.PATCH,"/api/beneficios/"+id+"/disponibilidad",Map.of(),dto,jwt);
    }
    @GetMapping("/categorias") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JsonNode> categorias(@RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamano,@AuthenticationPrincipal Jwt jwt) {
        return call(BENEFICIOS,HttpMethod.GET,"/api/categorias",page(pagina,tamano),null,jwt);
    }
    @PostMapping("/categorias") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JsonNode> crearCategoria(@Valid @RequestBody Requests.Categoria dto,@AuthenticationPrincipal Jwt jwt) {
        return call(BENEFICIOS,HttpMethod.POST,"/api/categorias",Map.of(),dto,jwt);
    }
    @PostMapping("/inscripciones") @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<JsonNode> inscribir(@Valid @RequestBody Requests.Inscripcion dto,@AuthenticationPrincipal Jwt jwt) {
        return call(INSCRIPCIONES,HttpMethod.POST,"/api/inscripciones",Map.of(),dto,jwt);
    }
    @GetMapping("/inscripciones/me") @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<JsonNode> propias(@RequestParam(defaultValue="0") int pagina,
            @RequestParam(defaultValue="20") int tamano,@AuthenticationPrincipal Jwt jwt) {
        return call(INSCRIPCIONES,HttpMethod.GET,"/api/inscripciones/me",page(pagina,tamano),null,jwt);
    }

    private ResponseEntity<JsonNode> state(String sub,String action,Jwt jwt) {
        // Identificadores Keycloak estándar; no se admiten segmentos de ruta.
        if(!sub.matches("[A-Za-z0-9_-]{1,255}")) throw new IllegalArgumentException("usuarioSub no válido");
        return call(USUARIOS,HttpMethod.PATCH,"/api/usuarios/"+sub+"/"+action,Map.of(),null,jwt);
    }
    private Map<String,Object> page(int pagina,int tamano) {
        if(pagina<0 || tamano<1 || tamano>100) throw new IllegalArgumentException("Página >= 0 y tamaño entre 1 y 100");
        return Map.of("pagina",pagina,"tamano",tamano);
    }
    private ResponseEntity<JsonNode> call(BusinessGateway.Destination destination,HttpMethod method,String path,
            Map<String,?> query,Object body,Jwt jwt) {
        return gateway.call(destination,method,path,query,body,jwt.getTokenValue());
    }
}
