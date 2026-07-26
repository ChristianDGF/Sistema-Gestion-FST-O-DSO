package proyecto.sistemaGestion.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Signs JWTs for security tests. The "trusted" key pair matches the JwtDecoder registered in
 * {@link proyecto.sistemaGestion.config.JwtDecoderTestConfig}; the "other" key pair simulates a
 * token forged/signed by a party that does not hold the API's trusted key.
 */
final class JwtTestSupport {

    static final String ISSUER = "http://localhost:8080/realms/sistema-gestion";

    static final RSAKey TRUSTED_KEY = generateKey();
    private static final RSAKey OTHER_KEY = generateKey();

    private JwtTestSupport() {
    }

    static RSAPublicKey trustedPublicKey() {
        try {
            return TRUSTED_KEY.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    static String validToken(String subject, List<String> permissions) {
        return sign(claims(subject, ISSUER, permissions, Instant.now().plusSeconds(300)), TRUSTED_KEY);
    }

    static String expiredToken(String subject, List<String> permissions) {
        return sign(claims(subject, ISSUER, permissions, Instant.now().minusSeconds(60)), TRUSTED_KEY);
    }

    static String wrongIssuerToken(String subject, List<String> permissions) {
        return sign(claims(subject, "http://attacker.example.com/realms/fake", permissions, Instant.now().plusSeconds(300)), TRUSTED_KEY);
    }

    static String forgedSignatureToken(String subject, List<String> permissions) {
        return sign(claims(subject, ISSUER, permissions, Instant.now().plusSeconds(300)), OTHER_KEY);
    }

    private static JWTClaimsSet claims(String subject, String issuer, List<String> permissions, Instant expiry) {
        return new JWTClaimsSet.Builder()
                .subject(subject)
                .issuer(issuer)
                .issueTime(Date.from(Instant.now()))
                .expirationTime(Date.from(expiry))
                .claim("permissions", permissions)
                .build();
    }

    private static String sign(JWTClaimsSet claims, RSAKey key) {
        try {
            SignedJWT signedJwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                    claims);
            signedJwt.sign(new RSASSASigner(key));
            return signedJwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RSAKey generateKey() {
        try {
            return new RSAKeyGenerator(2048).keyID("test-key").generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
