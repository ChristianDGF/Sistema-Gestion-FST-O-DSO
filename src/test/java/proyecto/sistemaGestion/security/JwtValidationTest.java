package proyecto.sistemaGestion.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real OAuth2 resource server filter chain (JwtDecoder + JwtAuthenticationConverter)
 * end to end, as opposed to the @WithMockUser-based controller tests which bypass JWT parsing
 * entirely. Covers the "Validacion JWT" and "Validacion de autenticacion" items required by the
 * project's Security Testing scope.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(JwtDecoderTestConfig.class)
class JwtValidationTest {

    private static final String PRODUCTS_ENDPOINT = "/api/v1/products";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldReturn401_whenNoAuthorizationHeaderPresent() throws Exception {
        mockMvc.perform(get(PRODUCTS_ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenTokenIsMalformed() throws Exception {
        mockMvc.perform(get(PRODUCTS_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenTokenIsExpired() throws Exception {
        String token = JwtTestSupport.expiredToken("user-1", List.of("product:view"));

        mockMvc.perform(get(PRODUCTS_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenIssuerDoesNotMatch() throws Exception {
        String token = JwtTestSupport.wrongIssuerToken("user-1", List.of("product:view"));

        mockMvc.perform(get(PRODUCTS_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401_whenSignatureIsNotTrusted() throws Exception {
        String token = JwtTestSupport.forgedSignatureToken("user-1", List.of("product:view"));

        mockMvc.perform(get(PRODUCTS_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403_whenTokenHasNoPermissionsClaim() throws Exception {
        String token = JwtTestSupport.validToken("user-1", List.of());

        mockMvc.perform(get(PRODUCTS_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn200_whenTokenIsValidWithRequiredScope() throws Exception {
        String token = JwtTestSupport.validToken("user-1", List.of("product:view"));

        mockMvc.perform(get(PRODUCTS_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }
}
