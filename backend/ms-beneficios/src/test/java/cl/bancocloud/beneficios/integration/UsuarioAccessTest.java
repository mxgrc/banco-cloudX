package cl.bancocloud.beneficios.integration;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
class UsuarioAccessTest {
    RestClient.Builder builder; MockRestServiceServer server; UsuarioAccess access;
    @BeforeEach void setup() {
        builder=RestClient.builder().baseUrl("http://usuarios.test");
        server=MockRestServiceServer.bindTo(builder).build(); access=new UsuarioAccess(builder.build());
    }
    @Test void propagaBearerYValidaActivo() {
        server.expect(requestTo("http://usuarios.test/api/usuarios/me")).andExpect(header("Authorization","Bearer token"))
            .andRespond(withSuccess("{\"usuarioSub\":\"cliente\",\"estado\":\"ACTIVO\"}",MediaType.APPLICATION_JSON));
        access.exigirActivo("cliente","token"); server.verify();
    }
    @Test void estadoInactivoRechaza() {
        server.expect(anything()).andRespond(withSuccess("{\"usuarioSub\":\"cliente\",\"estado\":\"INACTIVO\"}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> access.exigirActivo("cliente","token")).isInstanceOfSatisfying(ResponseStatusException.class,
            e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
    }
    @Test void sujetoAjenoNoAutoriza() {
        server.expect(anything()).andRespond(withSuccess("{\"usuarioSub\":\"otro\",\"estado\":\"ACTIVO\"}",MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> access.exigirActivo("cliente","token")).isInstanceOfSatisfying(ResponseStatusException.class,
            e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
    }
    @Test void errorRemotoNoAutoriza() {
        server.expect(anything()).andRespond(withServerError());
        assertThatThrownBy(() -> access.exigirActivo("cliente","token")).isInstanceOfSatisfying(ResponseStatusException.class,
            e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
    }
}
