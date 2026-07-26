package proyecto.sistemaGestion.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Replaces the JWK-set-backed JwtDecoder (which needs a reachable Keycloak) with one that trusts
 * only {@link JwtTestSupport#TRUSTED_KEY}, so tests can assert on signature/expiry/issuer
 * validation without a running Keycloak instance.
 */
@TestConfiguration
class JwtDecoderTestConfig {

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(JwtTestSupport.trustedPublicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtTestSupport.ISSUER));
        return decoder;
    }
}
