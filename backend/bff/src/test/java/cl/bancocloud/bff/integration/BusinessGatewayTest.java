package cl.bancocloud.bff.integration;
import java.net.SocketTimeoutException;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
class BusinessGatewayTest {
    MockRestServiceServer server; BusinessGateway gateway;
    @BeforeEach void setup() {
        var builder=RestClient.builder().baseUrl("http://local.test");
        server=MockRestServiceServer.bindTo(builder).build();
        var client=builder.build();
        gateway=new BusinessGateway(client,client,client,new ObjectMapper());
    }
    @ParameterizedTest @ValueSource(ints={200,201,401,403,404,409})
    void conservaCodigosDeNegocio(int code) {
        server.expect(requestTo("http://local.test/api/usuarios/me")).andExpect(header("Authorization","Bearer token"))
            .andRespond(withStatus(HttpStatus.valueOf(code)).contentType(MediaType.APPLICATION_JSON).body("{\"estado\":\"ACTIVO\"}"));
        var result=gateway.call(BusinessGateway.Destination.USUARIOS,HttpMethod.GET,"/api/usuarios/me",Map.of(),null,"token");
        assertThat(result.getStatusCode().value()).isEqualTo(code);
        assertThat(result.getBody().get("estado").asText()).isEqualTo("ACTIVO");
        if(code==401) assertThat(result.getHeaders().getFirst("WWW-Authenticate")).isEqualTo("Bearer");
        server.verify();
    }
    @Test void propagaPatchYNoReenviaCookies() {
        server.expect(requestTo("http://local.test/api/usuarios/cliente/aprobar"))
            .andExpect(method(HttpMethod.PATCH)).andExpect(header("Authorization","Bearer token"))
            .andRespond(withSuccess("{}",MediaType.APPLICATION_JSON).header("Set-Cookie","insegura=1").header("X-User","otro"));
        var result=gateway.call(BusinessGateway.Destination.USUARIOS,HttpMethod.PATCH,"/api/usuarios/cliente/aprobar",Map.of(),null,"token");
        assertThat(result.getHeaders().containsKey("Set-Cookie")).isFalse();
        assertThat(result.getHeaders().containsKey("X-User")).isFalse();
        server.verify();
    }
    @Test void enviaDtoYPaginacion() {
        server.expect(requestTo("http://local.test/api/beneficios?tamano=20"))
            .andExpect(method(HttpMethod.POST)).andExpect(content().json("{\"nombre\":\"Viajes\"}"))
            .andRespond(withSuccess("{}",MediaType.APPLICATION_JSON));
        gateway.call(BusinessGateway.Destination.BENEFICIOS,HttpMethod.POST,"/api/beneficios",Map.of("tamano",20),Map.of("nombre","Viajes"),"token");
        server.verify();
    }
    @ParameterizedTest @ValueSource(ints={302,500,503})
    void redireccionYErrorServidor502(int code) {
        server.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(code)));
        assertThatThrownBy(()->gateway.call(BusinessGateway.Destination.USUARIOS,HttpMethod.GET,"/api/usuarios/me",Map.of(),null,"token"))
            .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(502));
    }
    @Test void timeout503() {
        server.expect(anything()).andRespond(withException(new SocketTimeoutException()));
        assertThatThrownBy(()->gateway.call(BusinessGateway.Destination.USUARIOS,HttpMethod.GET,"/api/usuarios/me",Map.of(),null,"token"))
            .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(503));
    }
    @Test void jsonInvalido502() {
        server.expect(anything()).andRespond(withSuccess("<html>Error</html>",MediaType.TEXT_HTML));
        assertThatThrownBy(()->gateway.call(BusinessGateway.Destination.USUARIOS,HttpMethod.GET,"/api/usuarios/me",Map.of(),null,"token"))
            .isInstanceOfSatisfying(ResponseStatusException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(502));
    }
}
