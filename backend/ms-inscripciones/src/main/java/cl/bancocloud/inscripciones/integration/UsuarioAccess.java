package cl.bancocloud.inscripciones.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UsuarioAccess {
    private final RestClient client;
    UsuarioAccess(RestClient client) { this.client = client; }
    @org.springframework.beans.factory.annotation.Autowired
    public UsuarioAccess(RestClient.Builder builder, @Value("${banco.usuarios-url:http://localhost:8082}") String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client = builder.clone().baseUrl(url).requestFactory(factory).build();
    }
    public record Estado(String usuarioSub, String estado) {}
    public void exigirActivo(String sub, String token) {
        Estado estado;
        try {
            estado = client.get().uri("/api/usuarios/me").headers(h -> h.setBearerAuth(token))
                    .retrieve().body(Estado.class);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 404 || status == 403) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso al portal no autorizado");
            if (status == 401) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token rechazado por usuarios");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo verificar el estado del cliente");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de usuarios no disponible");
        }
        if (estado == null || !sub.equals(estado.usuarioSub())
                || !java.util.List.of("ACTIVO", "INACTIVO", "PENDIENTE").contains(estado.estado() == null ? "" : estado.estado())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Respuesta de usuarios no válida");
        }
        if (!"ACTIVO".equals(estado.estado())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El cliente debe estar ACTIVO");
    }
}
