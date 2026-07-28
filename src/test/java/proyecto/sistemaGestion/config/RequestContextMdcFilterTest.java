package proyecto.sistemaGestion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RequestContextMdcFilterTest {

    private final RequestContextMdcFilter filter = new RequestContextMdcFilter();

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void reusesIncomingCorrelationId() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Correlation-Id")).thenReturn("existing-id");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/products");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader("X-Correlation-Id", "existing-id");
        verify(chain).doFilter(request, response);
    }

    @Test
    void generatesCorrelationId_whenMissing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Correlation-Id")).thenReturn(null);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/products");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(eq("X-Correlation-Id"), argThat(id -> id != null && !id.isBlank()));
    }

    @Test
    void populatesMdc_duringChainExecution_andClearsAfter() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Correlation-Id")).thenReturn("cid-1");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/products");

        String[] endpointDuringChain = new String[1];
        doAnswer(invocation -> {
            endpointDuringChain[0] = MDC.get("endpoint");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertEquals("POST /api/v1/products", endpointDuringChain[0]);
        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("endpoint"));
        assertNull(MDC.get("user"));
    }

    @Test
    void resolvesUser_fromJwtPreferredUsername() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "u1")
                .claim("preferred_username", "admin")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Correlation-Id")).thenReturn("cid-2");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/dashboard");

        String[] userDuringChain = new String[1];
        doAnswer(invocation -> {
            userDuringChain[0] = MDC.get("user");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertEquals("admin", userDuringChain[0]);
    }

    @Test
    void resolvesUser_asAnonymous_whenUnauthenticated() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Correlation-Id")).thenReturn("cid-3");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/dashboard");

        String[] userDuringChain = new String[1];
        doAnswer(invocation -> {
            userDuringChain[0] = MDC.get("user");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilterInternal(request, response, chain);

        assertEquals("anonymous", userDuringChain[0]);
    }
}
