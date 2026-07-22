package proyecto.sistemaGestion.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles the external partner API (/api/external/**) per Keycloak client_id (azp claim).
 * Wired explicitly after BearerTokenAuthenticationFilter in SecurityConfig so the JWT is
 * already resolved into the SecurityContext when this filter runs.
 */
public class ExternalApiRateLimitFilter extends OncePerRequestFilter {

    private static final String EXTERNAL_API_PREFIX = "/api/external/";
    private static final int REQUESTS_PER_MINUTE = 60;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(EXTERNAL_API_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(resolveClientId(), id -> newBucket());
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"rate_limit_exceeded\",\"message\":\"Demasiadas solicitudes, intente nuevamente en un minuto\"}");
        }
    }

    private String resolveClientId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String clientId = jwtAuth.getToken().getClaimAsString("azp");
            if (clientId != null) {
                return clientId;
            }
        }
        return "anonymous";
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(REQUESTS_PER_MINUTE)
                        .refillGreedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                        .build())
                .build();
    }
}
