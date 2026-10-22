package cl.bancocloud.inscripciones;
import java.util.UUID;
import cl.bancocloud.inscripciones.integration.*;
import cl.bancocloud.inscripciones.repository.InscripcionRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class InscripcionesHttpTest {
    @Autowired MockMvc mvc;
    @Autowired InscripcionRepository repository;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean UsuarioAccess usuarios;
    @MockitoBean BeneficioAccess beneficios;
    @BeforeEach void clean() { repository.deleteAll(); }
    private org.springframework.test.web.servlet.request.RequestPostProcessor cliente(String sub) {
        return jwt().jwt(j -> j.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_CLIENTE"),new SimpleGrantedAuthority("SCOPE_banco-cloud-access"));
    }
    @Test void inscribeUnaVezYConsultaSoloPropias() throws Exception {
        String id=UUID.randomUUID().toString();
        for(String sub:java.util.List.of("cliente","otro")) {
            mvc.perform(post("/api/inscripciones").with(cliente(sub)).contentType("application/json")
                .content("{\"beneficioId\":\""+id+"\",\"usuarioSub\":\"intruso\"}")).andExpect(status().isCreated());
        }
        mvc.perform(post("/api/inscripciones").with(cliente("cliente")).contentType("application/json")
            .content("{\"beneficioId\":\""+id+"\"}")).andExpect(status().isConflict());
        mvc.perform(get("/api/inscripciones/me").with(cliente("cliente"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        assertThat(repository.count()).isEqualTo(2);
        assertThat(repository.existsByUsuarioSubAndBeneficioId("intruso",UUID.fromString(id))).isFalse();
        verify(usuarios,atLeastOnce()).exigirActivo(eq("cliente"),anyString());
        verify(beneficios,atLeastOnce()).exigirDisponible(eq(UUID.fromString(id)),anyString());
    }
    @Test void inactivoNoEscribeNiConsulta() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(usuarios).exigirActivo(anyString(),anyString());
        mvc.perform(post("/api/inscripciones").with(cliente("cliente")).contentType("application/json")
            .content("{\"beneficioId\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/inscripciones/me").with(cliente("cliente"))).andExpect(status().isForbidden());
        assertThat(repository.count()).isZero(); verifyNoInteractions(beneficios);
    }
    @Test void beneficioNoDisponibleYFalloRemotoNoEscriben() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT)).when(beneficios).exigirDisponible(any(),anyString());
        mvc.perform(post("/api/inscripciones").with(cliente("cliente")).contentType("application/json")
            .content("{\"beneficioId\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isConflict());
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(beneficios).exigirDisponible(any(),anyString());
        mvc.perform(post("/api/inscripciones").with(cliente("cliente")).contentType("application/json")
            .content("{\"beneficioId\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isServiceUnavailable());
        assertThat(repository.count()).isZero();
    }
    @Test void faltaTokenRolYBeneficio() throws Exception {
        mvc.perform(get("/api/inscripciones/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/inscripciones/me").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("SCOPE_banco-cloud-access")))).andExpect(status().isForbidden());
        mvc.perform(post("/api/inscripciones").with(cliente("cliente")).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }
}
