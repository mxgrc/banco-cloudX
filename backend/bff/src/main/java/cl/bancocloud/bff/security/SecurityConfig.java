package cl.bancocloud.bff.security;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    JwtDecoder jwtDecoder(@Value("${banco.security.issuer}") String issuer,
                          @Value("${banco.security.jwks}") String jwks,
                          @Value("${banco.security.audience}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks)
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        decoder.setJwtValidator(validators(issuer, audience));
        return decoder;
    }

    public static OAuth2TokenValidator<Jwt> validators(String issuer, String audience) {
        OAuth2TokenValidator<Jwt> requiredClaims = jwt -> {
            String sub = jwt.getSubject();
            if (sub == null || sub.isBlank() || sub.length() > 255
                    || jwt.getExpiresAt() == null || jwt.getAudience() == null || !jwt.getAudience().contains(audience)
                    || !"Bearer".equals(jwt.getClaimAsString("typ"))) {
                return OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Token de acceso no válido", null));
            }
            return OAuth2TokenValidatorResult.success();
        };
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), requiredClaims);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> result = new ArrayList<>(new JwtGrantedAuthoritiesConverter().convert(jwt));
            Object resources = jwt.getClaims().get("resource_access");
            if (resources instanceof Map<?, ?> map
                    && map.get("banco-cloud-api") instanceof Map<?, ?> api
                    && api.get("roles") instanceof Collection<?> roles) {
                for (String role : List.of("ADMIN", "CLIENTE")) {
                    if (roles.contains(role)) result.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
            return result;
        });
        // API Bearer sin cookies de autenticación; no usa la sesión del navegador.
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().hasAuthority("SCOPE_banco-cloud-access"))
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
                .build();
    }
}
