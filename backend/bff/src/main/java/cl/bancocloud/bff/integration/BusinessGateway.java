package cl.bancocloud.bff.integration;

import java.util.Map;
import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BusinessGateway {
    public enum Destination { USUARIOS, BENEFICIOS, INSCRIPCIONES }
    private final Map<Destination,RestClient> clients;
    private final ObjectMapper json;
    public BusinessGateway(@Qualifier("usuariosClient") RestClient usuarios,
            @Qualifier("beneficiosClient") RestClient beneficios,
            @Qualifier("inscripcionesClient") RestClient inscripciones,ObjectMapper json) {
        clients=Map.of(Destination.USUARIOS,usuarios,Destination.BENEFICIOS,beneficios,Destination.INSCRIPCIONES,inscripciones);
        this.json=json;
    }
    public ResponseEntity<JsonNode> call(Destination destination,HttpMethod method,String path,
            Map<String,?> query,Object body,String token) {
        try {
            var request=clients.get(destination).method(method).uri(builder -> {
                builder.path(path); query.forEach(builder::queryParam); return builder.build();
            }).headers(h -> h.setBearerAuth(token)).accept(MediaType.APPLICATION_JSON);
            if(body!=null) request.contentType(MediaType.APPLICATION_JSON).body(body);
            return request.exchange((req,response) -> {
                int status=response.getStatusCode().value();
                if(status>=500) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"El microservicio no pudo completar la operación");
                if(status>=300 && status<400) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Redirección inesperada del microservicio");
                byte[] bytes=response.getBody().readNBytes(1_048_577);
                if(bytes.length>1_048_576) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Respuesta demasiado grande");
                JsonNode result=null;
                if(bytes.length>0) {
                    try { result=json.readTree(bytes); }
                    catch(java.io.IOException ex) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Respuesta no JSON del microservicio"); }
                }
                var output=ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON);
                if(status==401) output.header(HttpHeaders.WWW_AUTHENTICATE,"Bearer");
                // No se propagan cookies, cabeceras de identidad ni cabeceras arbitrarias.
                return output.body(result);
            });
        } catch(RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No hay conexión con el microservicio");
        }
    }
}
