package cl.bancocloud.inscripciones.integration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BeneficioAccess {
    private final RestClient client;
    BeneficioAccess(RestClient client) { this.client = client; }
    @org.springframework.beans.factory.annotation.Autowired
    public BeneficioAccess(RestClient.Builder builder, @Value("${banco.beneficios-url:http://localhost:8081}") String url) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); factory.setReadTimeout(3000);
        client = builder.clone().baseUrl(url).requestFactory(factory).build();
    }
    public record Disponibilidad(UUID id, Boolean disponible) {}
    public void exigirDisponible(UUID id, String token) {
        Disponibilidad dto;
        try {
            dto=client.get().uri("/api/beneficios/{id}/disponibilidad",id)
                    .headers(h -> h.setBearerAuth(token)).retrieve().body(Disponibilidad.class);
        } catch (RestClientResponseException ex) {
            int status=ex.getStatusCode().value();
            if (status==401 || status==403 || status==404 || status==409)
                throw new ResponseStatusException(HttpStatus.valueOf(status), "Beneficio no accesible para inscripción");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo verificar el beneficio");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de beneficios no disponible");
        }
        if(dto==null || !id.equals(dto.id()) || dto.disponible()==null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Respuesta de beneficios no válida");
        if(!dto.disponible()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Beneficio no disponible");
    }
}
