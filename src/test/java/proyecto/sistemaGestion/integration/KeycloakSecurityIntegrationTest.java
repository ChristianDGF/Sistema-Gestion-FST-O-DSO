package proyecto.sistemaGestion.integration;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;

import java.io.IOException;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unlike {@code security.JwtValidationTest} (which trusts a simulated key via
 * {@code JwtDecoderTestConfig}), this exercises the full real chain: a live Keycloak issues the
 * JWT, Spring Security fetches its actual JWKS, and {@code SecurityConfig}'s
 * {@code @PreAuthorize} checks run against it end to end.
 */
class KeycloakSecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String REALM = "sistema-gestion";
    private static final String CLIENT_ID = "sistema-gestion-client";

    @Container
    static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.1.0")
            .withRealmImportFile("/sistema-gestion-realm.json")
            // Default startup timeout is too tight under CI/Docker Desktop resource
            // contention, causing intermittent "Timed out waiting for URL" failures.
            .withStartupTimeout(Duration.ofMinutes(3));

    @BeforeAll
    static void startKeycloak() {
        keycloak.start();
    }

    @DynamicPropertySource
    static void configureIssuer(DynamicPropertyRegistry registry) {
        String issuerUri = keycloak.getAuthServerUrl() + "/realms/" + REALM;
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> issuerUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> issuerUri + "/protocol/openid-connect/certs");
    }

    @LocalServerPort
    private int port;

    // Default RestTemplate throws on 4xx/5xx; these tests assert on 401/403 responses directly,
    // so the error handler is disarmed (same behavior TestRestTemplate gives out of the box).
    private final RestTemplate restTemplate = nonThrowingRestTemplate();

    private static RestTemplate nonThrowingRestTemplate() {
        RestTemplate template = new RestTemplate();
        template.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return false;
            }
        });
        return template;
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private String tokenFor(String username, String password) {
        String tokenUrl = keycloak.getAuthServerUrl() + "/realms/" + REALM + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", CLIENT_ID);
        form.add("username", username);
        form.add("password", password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = new RestTemplate()
                .postForEntity(tokenUrl, new HttpEntity<>(form, headers), Map.class);
        return (String) response.getBody().get("access_token");
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void adminToken_canListProducts() {
        String token = tokenFor("admin", "admin123");

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/api/v1/products"), HttpMethod.GET, new HttpEntity<>(bearerHeaders(token)), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void employeeToken_cannotCreateProduct() {
        String token = tokenFor("employee", "employee123");

        HttpHeaders headers = bearerHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"name":"Blocked","sku":"KC-EMP-001","category":"Test","price":10,"quantity":1,"minStock":0}
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl("/api/v1/products"), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void noToken_isUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl("/api/v1/products"), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
