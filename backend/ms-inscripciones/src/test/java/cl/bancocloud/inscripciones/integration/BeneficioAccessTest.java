package cl.bancocloud.inscripciones.integration;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
class BeneficioAccessTest {
    MockRestServiceServer server; BeneficioAccess access; UUID id=UUID.randomUUID();
    @BeforeEach void setup() {
        var builder=RestClient.builder().baseUrl("http://beneficios.test");
        server=MockRestServiceServer.bindTo(builder).build(); access=new BeneficioAccess(builder.build());
    }
    @Test void propagaTokenYConsultaIdExacto() {
        server.expect(requestTo("http://beneficios.test/api/beneficios/"+id+"/disponibilidad"))
            .andExpect(header("Authorization","Bearer token"))
            .andRespond(withSuccess("{\"id\":\""+id+"\",\"disponible\":true}",MediaType.APPLICATION_JSON));
        access.exigirDisponible(id,"token"); server.verify();
    }
    @Test void noDisponibleRechaza() {
        server.expect(anything()).andRespond(withSuccess("{\"id\":\""+id+"\",\"disponible\":false}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> access.exigirDisponible(id,"token")).isInstanceOfSatisfying(ResponseStatusException.class,
            e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }
    @Test void respuestaVaciaRechaza() {
        server.expect(anything()).andRespond(withNoContent());
        assertThatThrownBy(() -> access.exigirDisponible(id,"token")).isInstanceOfSatisfying(ResponseStatusException.class,
            e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
    }
}
