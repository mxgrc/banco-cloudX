package cl.bancocloud.beneficios;
import cl.bancocloud.beneficios.integration.UsuarioAccess;
import cl.bancocloud.beneficios.repository.*;
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
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class BeneficiosHttpTest {
    @Autowired MockMvc mvc;
    @Autowired BeneficioRepository beneficios;
    @Autowired CategoriaRepository categorias;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean UsuarioAccess acceso;
    @BeforeEach void clean() { beneficios.deleteAll(); categorias.deleteAll(); }
    private org.springframework.test.web.servlet.request.RequestPostProcessor role(String role) {
        return jwt().jwt(j -> j.subject("cliente")).authorities(new SimpleGrantedAuthority("ROLE_"+role),new SimpleGrantedAuthority("SCOPE_banco-cloud-access"));
    }
    private String crear() throws Exception {
        var result=mvc.perform(post("/api/beneficios").with(role("ADMIN")).contentType("application/json")
            .content("{\"nombre\":\"Viajes\",\"descripcion\":\"Descuento\",\"descuento\":15}"))
            .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
    @Test void sinToken401YClienteNoCrea() throws Exception {
        mvc.perform(get("/api/beneficios")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/beneficios").with(role("CLIENTE")).contentType("application/json")
            .content("{\"nombre\":\"X\",\"descripcion\":\"D\",\"descuento\":10}")).andExpect(status().isForbidden());
    }
    @Test void creaAsignaCategoriaYFiltraDisponibles() throws Exception {
        String id=crear();
        var r=mvc.perform(post("/api/categorias").with(role("ADMIN")).contentType("application/json").content("{\"nombre\":\"Turismo\"}"))
            .andExpect(status().isCreated()).andReturn();
        String categoria=json.readTree(r.getResponse().getContentAsString()).get("id").asText();
        mvc.perform(put("/api/beneficios/"+id+"/categoria").with(role("ADMIN")).contentType("application/json")
            .content("{\"categoriaId\":\""+categoria+"\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.categoriaId").value(categoria));
        mvc.perform(patch("/api/beneficios/"+id+"/disponibilidad").with(role("ADMIN")).contentType("application/json")
            .content("{\"disponible\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/beneficios/disponibles").with(role("CLIENTE"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/beneficios").with(role("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/beneficios/"+id+"/disponibilidad").with(role("CLIENTE"))).andExpect(status().isConflict());
    }
    @Test void inactivo403YServicioCaido503() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(acceso).exigirActivo(anyString(),anyString());
        mvc.perform(get("/api/beneficios/disponibles").with(role("CLIENTE"))).andExpect(status().isForbidden());
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(acceso).exigirActivo(anyString(),anyString());
        mvc.perform(get("/api/beneficios/disponibles").with(role("CLIENTE"))).andExpect(status().isServiceUnavailable());
    }
    @Test void validacionDescuentoYCategoriaInexistente() throws Exception {
        mvc.perform(post("/api/beneficios").with(role("ADMIN")).contentType("application/json")
            .content("{\"nombre\":\"X\",\"descripcion\":\"D\",\"descuento\":101}")).andExpect(status().isBadRequest());
        String id=crear();
        mvc.perform(put("/api/beneficios/"+id+"/categoria").with(role("ADMIN")).contentType("application/json")
            .content("{\"categoriaId\":\""+java.util.UUID.randomUUID()+"\"}")).andExpect(status().isNotFound());
    }
}
