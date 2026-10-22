package cl.bancocloud.bff;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import cl.bancocloud.bff.security.SecurityConfig;
import cl.bancocloud.bff.integration.BusinessGateway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Import(BffSecurityTest.Keys.class)
class BffSecurityTest {
    static final String ISSUER="http://localhost:8080/realms/banco-cloud";
    static final RSAKey KEY;
    static { try { KEY=new RSAKeyGenerator(2048).keyID("test").generate(); } catch(Exception e) {throw new ExceptionInInitializerError(e);} }
    @TestConfiguration static class Keys {
        @Bean @Primary JwtDecoder testDecoder() throws Exception {
            var decoder=NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
            decoder.setJwtValidator(SecurityConfig.validators(ISSUER,"banco-cloud-api"));
            return decoder;
        }
    }
    @Autowired MockMvc mvc;
    @MockitoBean BusinessGateway gateway;
    @BeforeEach void setup() {
        when(gateway.call(any(),any(),anyString(),anyMap(),any(),anyString()))
            .thenReturn(ResponseEntity.ok(JsonNodeFactory.instance.objectNode()));
    }
    String token(String role,Map<String,Object> overrides,RSAKey key) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer(ISSUER).subject("cliente-sub")
            .audience("banco-cloud-api").issueTime(Date.from(Instant.now().minusSeconds(600)))
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .claim("typ","Bearer").claim("scope","openid banco-cloud-access")
            .claim("resource_access",Map.of("banco-cloud-api",Map.of("roles",List.of(role))));
        overrides.forEach(claims::claim);
        var signed=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test").build(),claims.build());
        signed.sign(new RSASSASigner(key)); return signed.serialize();
    }
    static Stream<Arguments> routes() {
        String id="00000000-0000-0000-0000-000000000001";
        return Stream.of(
            Arguments.of("POST","/api/usuarios/me","{}","CLIENTE"),
            Arguments.of("GET","/api/usuarios/me","","CLIENTE"),
            Arguments.of("GET","/api/usuarios","","ADMIN"),
            Arguments.of("PATCH","/api/usuarios/cliente-sub/aprobar","","ADMIN"),
            Arguments.of("PATCH","/api/usuarios/cliente-sub/desactivar","","ADMIN"),
            Arguments.of("PATCH","/api/usuarios/cliente-sub/reactivar","","ADMIN"),
            Arguments.of("GET","/api/beneficios","","ADMIN"),
            Arguments.of("POST","/api/beneficios","{\"nombre\":\"Viajes\",\"descripcion\":\"Prueba\",\"descuento\":15}","ADMIN"),
            Arguments.of("GET","/api/beneficios/disponibles","","CLIENTE"),
            Arguments.of("GET","/api/beneficios/"+id+"/disponibilidad","","CLIENTE"),
            Arguments.of("PUT","/api/beneficios/"+id+"/categoria","{\"categoriaId\":\""+id+"\"}","ADMIN"),
            Arguments.of("PATCH","/api/beneficios/"+id+"/disponibilidad","{\"disponible\":false}","ADMIN"),
            Arguments.of("GET","/api/categorias","","ADMIN"),
            Arguments.of("POST","/api/categorias","{\"nombre\":\"Turismo\"}","ADMIN"),
            Arguments.of("POST","/api/inscripciones","{\"beneficioId\":\""+id+"\"}","CLIENTE"),
            Arguments.of("GET","/api/inscripciones/me","","CLIENTE")
        );
    }
    @ParameterizedTest @MethodSource("routes")
    void autorizaRolCorrecto(String method,String path,String body,String role) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method),path).header("Authorization","Bearer "+token(role,Map.of(),KEY))
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        verify(gateway).call(any(),any(),anyString(),anyMap(),any(),anyString());
    }
    @ParameterizedTest @MethodSource("routes")
    void rechazaRolContrarioAntesDeLlamarServicio(String method,String path,String body,String role) throws Exception {
        String other=role.equals("ADMIN")?"CLIENTE":"ADMIN";
        mvc.perform(request(HttpMethod.valueOf(method),path).header("Authorization","Bearer "+token(other,Map.of(),KEY))
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        verifyNoInteractions(gateway);
    }
    @Test void sinTokenNiCabecerasInventadas401() throws Exception {
        mvc.perform(get("/api/usuarios").header("X-User","admin").header("X-Roles","ADMIN"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(gateway);
    }
    @ParameterizedTest @ValueSource(strings={"iss","aud","exp","sin-exp","sin-aud","sub","typ","nbf"})
    void claimsInvalidos401(String claim) throws Exception {
        Map<String,Object> overrides=new HashMap<>();
        switch(claim) {
            case "iss" -> overrides.put("iss","http://other.test");
            case "aud" -> overrides.put("aud",List.of("otra-api"));
            case "exp" -> overrides.put("exp",Date.from(Instant.now().minusSeconds(120)));
            case "sin-exp" -> overrides.put("exp",null);
            case "sin-aud" -> overrides.put("aud",null);
            case "sub" -> overrides.put("sub","");
            case "typ" -> overrides.put("typ","ID");
            case "nbf" -> overrides.put("nbf",Date.from(Instant.now().plusSeconds(120)));
        }
        mvc.perform(get("/api/usuarios").header("Authorization","Bearer "+token("ADMIN",overrides,KEY)))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(gateway);
    }
    @Test void firmaIncorrecta401() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization","Bearer "+token("ADMIN",Map.of(),new RSAKeyGenerator(2048).generate())))
            .andExpect(status().isUnauthorized()); verifyNoInteractions(gateway);
    }
    @Test void scopeIncorrecto403() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization","Bearer "+token("ADMIN",Map.of("scope","openid"),KEY)))
            .andExpect(status().isForbidden()); verifyNoInteractions(gateway);
    }
    @Test void rolDeOtroClienteNoOtorgaPermisos() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization","Bearer "+token("ADMIN",
            Map.of("resource_access",Map.of("otro-cliente",Map.of("roles",List.of("ADMIN")))),KEY)))
            .andExpect(status().isForbidden()); verifyNoInteractions(gateway);
    }
    @Test void propagaExactamenteElTokenValidado() throws Exception {
        String access=token("CLIENTE",Map.of(),KEY);
        mvc.perform(get("/api/usuarios/me").header("Authorization","Bearer "+access).header("X-User","otro"))
            .andExpect(status().isOk());
        verify(gateway).call(eq(BusinessGateway.Destination.USUARIOS),eq(HttpMethod.GET),eq("/api/usuarios/me"),eq(Map.of()),isNull(),eq(access));
    }
    @Test void dtoYPaginacionSeValidanAntesDeEnviar() throws Exception {
        String admin="Bearer "+token("ADMIN",Map.of(),KEY);
        mvc.perform(get("/api/usuarios?tamano=101").header("Authorization",admin)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/beneficios").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nombre\":\"X\",\"descripcion\":\"Y\",\"descuento\":101}")).andExpect(status().isBadRequest());
        verifyNoInteractions(gateway);
    }
}
