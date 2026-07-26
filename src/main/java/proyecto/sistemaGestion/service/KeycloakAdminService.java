package proyecto.sistemaGestion.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.UserCreateRequest;
import proyecto.sistemaGestion.dto.UserResponse;
import proyecto.sistemaGestion.dto.UserUpdateRequest;
import proyecto.sistemaGestion.exception.BusinessException;
import proyecto.sistemaGestion.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * No existe una entidad User local: la autenticacion es 100% Keycloak. Este servicio actua como
 * proxy hacia la Admin REST API de Keycloak usando una cuenta de servicio propia
 * (sistema-gestion-admin-client, rol de cliente manage-users) en vez de reutilizar el client
 * publico del login de usuarios.
 */
@Service
public class KeycloakAdminService {

    private static final List<String> ASSIGNABLE_ROLES = List.of("admin", "employee");

    private final RestClient restClient;
    private final String realm;
    private final String adminClientId;
    private final String adminClientSecret;
    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();

    public KeycloakAdminService(RestClient.Builder restClientBuilder,
                                 @Value("${app.keycloak.auth-server-url}") String authServerUrl,
                                 @Value("${app.keycloak.realm}") String realm,
                                 @Value("${app.keycloak.admin-client-id}") String adminClientId,
                                 @Value("${app.keycloak.admin-client-secret}") String adminClientSecret) {
        this.restClient = restClientBuilder.baseUrl(authServerUrl).build();
        this.realm = realm;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
    }

    public PageResponse<UserResponse> listUsers(int page, int size, String search) {
        List<KeycloakUserRepresentation> users = restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/admin/realms/{realm}/users")
                            .queryParam("first", page * size)
                            .queryParam("max", size)
                            .queryParam("briefRepresentation", true);
                    if (search != null && !search.isBlank()) {
                        builder.queryParam("search", search);
                    }
                    return builder.build(realm);
                })
                .headers(this::authHeader)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<KeycloakUserRepresentation>>() {});

        Long total = restClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/admin/realms/{realm}/users/count");
                    if (search != null && !search.isBlank()) {
                        builder.queryParam("search", search);
                    }
                    return builder.build(realm);
                })
                .headers(this::authHeader)
                .retrieve()
                .body(Long.class);

        List<UserResponse> content = (users == null ? List.<KeycloakUserRepresentation>of() : users).stream()
                .map(u -> toUserResponse(u, findAssignedRole(u.getId())))
                .toList();

        long totalElements = total == null ? content.size() : total;
        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) totalElements / size);

        return PageResponse.<UserResponse>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(Math.max(totalPages, 1))
                .last(page + 1 >= totalPages)
                .build();
    }

    public UserResponse createUser(UserCreateRequest request) {
        if (!ASSIGNABLE_ROLES.contains(request.getRole())) {
            throw new BusinessException("El rol debe ser 'admin' o 'employee'");
        }

        KeycloakUserRepresentation payload = new KeycloakUserRepresentation();
        payload.setUsername(request.getUsername());
        payload.setEmail(request.getEmail());
        payload.setFirstName(request.getFirstName());
        payload.setLastName(request.getLastName());
        payload.setEnabled(true);

        KeycloakCredentialRepresentation credential = new KeycloakCredentialRepresentation();
        credential.setType("password");
        credential.setValue(request.getPassword());
        credential.setTemporary(false);
        payload.setCredentials(List.of(credential));

        ResponseEntity<Void> response = withErrorTranslation(() -> restClient.post()
                .uri("/admin/realms/{realm}/users", realm)
                .headers(this::authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity());

        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        if (location == null) {
            throw new BusinessException("Keycloak no devolvió la ubicación del usuario creado");
        }
        String userId = location.substring(location.lastIndexOf('/') + 1);

        assignRole(userId, request.getRole());

        return toUserResponse(fetchUser(userId), request.getRole());
    }

    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        if (!ASSIGNABLE_ROLES.contains(request.getRole())) {
            throw new BusinessException("El rol debe ser 'admin' o 'employee'");
        }

        KeycloakUserRepresentation current = fetchUser(userId);
        current.setEmail(request.getEmail());
        current.setFirstName(request.getFirstName());
        current.setLastName(request.getLastName());
        current.setEnabled(request.getEnabled());

        withErrorTranslation(() -> restClient.put()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(this::authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(current)
                .retrieve()
                .toBodilessEntity());

        String previousRole = findAssignedRole(userId);
        if (previousRole != null && !previousRole.equals(request.getRole())) {
            removeRole(userId, previousRole);
        }
        if (!request.getRole().equals(previousRole)) {
            assignRole(userId, request.getRole());
        }

        return toUserResponse(fetchUser(userId), request.getRole());
    }

    public void deleteUser(String userId) {
        guardAgainstSelf(userId, "No puedes eliminar tu propio usuario");

        withErrorTranslation(() -> restClient.delete()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(this::authHeader)
                .retrieve()
                .toBodilessEntity());
    }

    private void guardAgainstSelf(String userId, String message) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth
                && jwtAuth.getToken().getSubject().equals(userId)) {
            throw new BusinessException(message);
        }
    }

    private KeycloakUserRepresentation fetchUser(String userId) {
        return withErrorTranslation(() -> restClient.get()
                .uri("/admin/realms/{realm}/users/{id}", realm, userId)
                .headers(this::authHeader)
                .retrieve()
                .body(KeycloakUserRepresentation.class));
    }

    private String findAssignedRole(String userId) {
        List<KeycloakRoleRepresentation> roles = restClient.get()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                .headers(this::authHeader)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<KeycloakRoleRepresentation>>() {});
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(KeycloakRoleRepresentation::getName)
                .filter(ASSIGNABLE_ROLES::contains)
                .findFirst()
                .orElse(null);
    }

    private void assignRole(String userId, String roleName) {
        KeycloakRoleRepresentation role = fetchRealmRole(roleName);
        restClient.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                .headers(this::authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    private void removeRole(String userId, String roleName) {
        KeycloakRoleRepresentation role = fetchRealmRole(roleName);
        restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                .headers(this::authHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    private KeycloakRoleRepresentation fetchRealmRole(String roleName) {
        return restClient.get()
                .uri("/admin/realms/{realm}/roles/{roleName}", realm, roleName)
                .headers(this::authHeader)
                .retrieve()
                .body(KeycloakRoleRepresentation.class);
    }

    private <T> T withErrorTranslation(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (HttpClientErrorException.Conflict ex) {
            throw new BusinessException("El nombre de usuario o el email ya existe");
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Usuario no encontrado en Keycloak");
        }
    }

    private void authHeader(HttpHeaders headers) {
        headers.setBearerAuth(getServiceAccountToken());
    }

    private String getServiceAccountToken() {
        CachedToken current = cachedToken.get();
        if (current != null && current.expiresAt().isAfter(Instant.now())) {
            return current.accessToken();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", adminClientId);
        form.add("client_secret", adminClientSecret);

        TokenResponse response = restClient.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.getAccessToken() == null) {
            throw new BusinessException("No se pudo autenticar contra Keycloak Admin API");
        }

        // Se descuentan 30s de margen para evitar usar un token que expire durante la llamada.
        Instant expiresAt = Instant.now().plusSeconds(Math.max(response.getExpiresIn() - 30, 5));
        cachedToken.set(new CachedToken(response.getAccessToken(), expiresAt));
        return response.getAccessToken();
    }

    private UserResponse toUserResponse(KeycloakUserRepresentation user, String role) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .role(role)
                .createdTimestamp(user.getCreatedTimestamp())
                .build();
    }

    private record CachedToken(String accessToken, Instant expiresAt) {}

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KeycloakUserRepresentation {
        private String id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private boolean enabled;
        private Long createdTimestamp;
        private List<KeycloakCredentialRepresentation> credentials;
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KeycloakCredentialRepresentation {
        private String type;
        private String value;
        private boolean temporary;
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class KeycloakRoleRepresentation {
        private String id;
        private String name;
    }

    @Getter @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        @JsonProperty("expires_in")
        private long expiresIn;
    }
}
