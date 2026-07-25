package proyecto.sistemaGestion.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Populates the MDC with correlationId, user and endpoint so every log line emitted while
 * handling a request can be correlated across services and attributed to a caller. Wired after
 * BearerTokenAuthenticationFilter in SecurityConfig so the JWT is already resolved.
 */
public class RequestContextMdcFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            MDC.put("correlationId", correlationId);
            MDC.put("endpoint", request.getMethod() + " " + request.getRequestURI());
            MDC.put("user", resolveUser());
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String username = jwtAuth.getToken().getClaimAsString("preferred_username");
            if (username != null) {
                return username;
            }
            return jwtAuth.getToken().getSubject();
        }
        return "anonymous";
    }
}
