package cl.bancocloud.usuarios;

import java.time.Instant;
import java.util.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import cl.bancocloud.usuarios.repository.UsuarioPortalRepository;
import cl.bancocloud.usuarios.security.SecurityConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.jwt.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(UsuariosHttpTest.Keys.class)
class UsuariosHttpTest {
    private static final RSAKey KEY;
    private static final String ISSUER = "https://issuer.test/realms/banco-cloud";

    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (Exception error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    @TestConfiguration
    static class Keys {
        @Bean @Primary
        JwtDecoder testDecoder() throws Exception {
            // Firma real y mismos validadores; solo sustituye la descarga JWKS.
            NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
            decoder.setJwtValidator(SecurityConfig.validators(ISSUER, "banco-cloud-api"));
            return decoder;
        }
    }

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry props) {
        props.add("banco.security.issuer", () -> ISSUER);
        props.add("banco.security.audience", () -> "banco-cloud-api");
        props.add("banco.security.jwks", () -> "https://issuer.test/certs");
    }

    @Autowired MockMvc mvc;
    @Autowired UsuarioPortalRepository repository;

    @BeforeEach void clean() { repository.deleteAll(); }

    private String token(String sub, String role) throws Exception {
        return token(sub, role, Map.of(), KEY);
    }

    private String token(String sub, String role, Map<String, Object> overrides, RSAKey signingKey) throws Exception {
        var claims = new JWTClaimsSet.Builder()
                .issuer(ISSUER).subject(sub).audience("banco-cloud-api")
                .issueTime(Date.from(Instant.now().minusSeconds(5)))
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .claim("typ", "Bearer")
                .claim("scope", "openid banco-cloud-access")
                .claim("resource_access", Map.of("banco-cloud-api", Map.of("roles", List.of(role))));
        overrides.forEach(claims::claim);
        var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-key").build(), claims.build());
        jwt.sign(new RSASSASigner(signingKey));
        return "Bearer " + jwt.serialize();
    }

    private void register(String sub) throws Exception {
        mvc.perform(post("/api/usuarios/me").header("Authorization", token(sub, "CLIENTE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test void sinTokenEs401() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }

    @Test void firmaInvalidaEs401() throws Exception {
        RSAKey other = new RSAKeyGenerator(2048).generate();
        mvc.perform(get("/api/usuarios").header("Authorization", token("admin", "ADMIN", Map.of(), other)))
                .andExpect(status().isUnauthorized());
    }

    @Test void issuerInvalidoEs401() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization",
                token("admin", "ADMIN", Map.of("iss", "https://other.test"), KEY)))
                .andExpect(status().isUnauthorized());
    }

    @Test void audienciaInvalidaEs401() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization",
                token("admin", "ADMIN", Map.of("aud", List.of("other-api")), KEY)))
                .andExpect(status().isUnauthorized());
    }

    @Test void expiradoEs401() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization",
                token("admin", "ADMIN", Map.of("exp", Date.from(Instant.now().minusSeconds(120))), KEY)))
                .andExpect(status().isUnauthorized());
    }

    @Test void idTokenNoSeAceptaComoAccessToken() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization",
                token("admin", "ADMIN", Map.of("typ", "ID"), KEY)))
                .andExpect(status().isUnauthorized());
    }

    @Test void scopeFaltanteEs403() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization",
                token("admin", "ADMIN", Map.of("scope", "openid"), KEY)))
                .andExpect(status().isForbidden());
    }

    @Test void clienteNoListaNiApruebaYAdminNoEsCliente() throws Exception {
        register("cliente");
        String cliente = token("cliente", "CLIENTE");
        mvc.perform(get("/api/usuarios").header("Authorization", cliente)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/usuarios/cliente/aprobar").header("Authorization", cliente))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios/me").header("Authorization", token("admin", "ADMIN")))
                .andExpect(status().isForbidden());
        assertThat(repository.findById("cliente").orElseThrow().isActivo()).isFalse();
    }

    @Test void registroDerivaSubDelTokenYEsRepetible() throws Exception {
        String cliente = token("cliente", "CLIENTE");
        mvc.perform(post("/api/usuarios/me").header("Authorization", cliente)
                        .contentType("application/json").content("{\"usuarioSub\":\"otro\",\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuarioSub").value("cliente"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        register("cliente");
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.existsById("otro")).isFalse();
    }

    @Test void cicloDeEstadosYAuditoria() throws Exception {
        register("cliente");
        String admin = token("administrador-sub", "ADMIN");
        String cliente = token("cliente", "CLIENTE");
        mvc.perform(patch("/api/usuarios/cliente/aprobar").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.modificadoPorSub").value("administrador-sub"));
        mvc.perform(patch("/api/usuarios/cliente/desactivar").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("INACTIVO"));
        mvc.perform(post("/api/usuarios/me").header("Authorization", cliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("INACTIVO"));
        mvc.perform(get("/api/usuarios/me").header("Authorization", cliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("INACTIVO"));
        mvc.perform(patch("/api/usuarios/cliente/reactivar").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test void transicionInvalidaEs409() throws Exception {
        register("cliente");
        mvc.perform(patch("/api/usuarios/cliente/reactivar").header("Authorization", token("admin", "ADMIN")))
                .andExpect(status().isConflict());
    }

    @Test void perfilAjenoNoSeObtieneConMe() throws Exception {
        register("otro");
        mvc.perform(get("/api/usuarios/me").header("Authorization", token("cliente", "CLIENTE")))
                .andExpect(status().isNotFound());
    }

    @Test void adminPuedeFiltrarYLaPaginacionSeValida() throws Exception {
        register("cliente");
        String admin = token("admin", "ADMIN");
        mvc.perform(get("/api/usuarios?estado=PENDIENTE").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.contenido[0].usuarioSub").value("cliente"));
        mvc.perform(get("/api/usuarios?tamano=101").header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/usuarios?estado=OTRO").header("Authorization", admin))
                .andExpect(status().isBadRequest());
    }
}
