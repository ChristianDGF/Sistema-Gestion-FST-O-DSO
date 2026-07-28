package proyecto.sistemaGestion.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.UserCreateRequest;
import proyecto.sistemaGestion.dto.UserResponse;
import proyecto.sistemaGestion.dto.UserUpdateRequest;
import proyecto.sistemaGestion.exception.BusinessException;

import java.net.URI;
import java.time.Instant;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class KeycloakAdminServiceTest {

    private static final String AUTH_SERVER_URL = "http://keycloak-test:8080";
    private static final String TOKEN_RESPONSE = """
            {"access_token":"service-account-token","expires_in":300}
            """;

    private MockRestServiceServer mockServer;
    private KeycloakAdminService keycloakAdminService;

    @BeforeEach
    void setup() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        keycloakAdminService = new KeycloakAdminService(builder, AUTH_SERVER_URL, "sistema-gestion",
                "sistema-gestion-admin-client", "test-secret");
        SecurityContextHolder.clearContext();
    }

    private void expectTokenFetch() {
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/realms/sistema-gestion/protocol/openid-connect/token"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
    }

    @Test
    void listUsers_mapsKeycloakUsersWithAssignedRole() {
        expectTokenFetch();
        mockServer.expect(requestTo(startsWith(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users?")))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":"u1","username":"admin","email":"admin@sistema.com","firstName":"Admin",
                          "lastName":"Sistema","enabled":true,"createdTimestamp":1700000000000}]
                        """, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(startsWith(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users/count")))
                .andRespond(withSuccess("1", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users/u1/role-mappings/realm"))
                .andRespond(withSuccess("""
                        [{"id":"r1","name":"admin"}]
                        """, MediaType.APPLICATION_JSON));

        PageResponse<UserResponse> result = keycloakAdminService.listUsers(0, 20, null);

        assertEquals(1, result.getContent().size());
        UserResponse user = result.getContent().get(0);
        assertEquals("admin", user.getUsername());
        assertEquals("admin", user.getRole());
        assertEquals(1, result.getTotalElements());
        mockServer.verify();
    }

    @Test
    void createUser_rejectsInvalidRole() {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("newuser").email("new@sistema.com").password("password123").role("superadmin").build();

        assertThrows(BusinessException.class, () -> keycloakAdminService.createUser(request));
    }

    @Test
    void createUser_conflictFromKeycloak_translatesToBusinessException() {
        expectTokenFetch();
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT));

        UserCreateRequest request = UserCreateRequest.builder()
                .username("admin").email("admin@sistema.com").password("password123").role("admin").build();

        BusinessException ex = assertThrows(BusinessException.class, () -> keycloakAdminService.createUser(request));
        assertTrue(ex.getMessage().contains("ya existe"));
    }

    @Test
    void deleteUser_refusesToDeleteTheAuthenticatedUser() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "own-id")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThrows(BusinessException.class, () -> keycloakAdminService.deleteUser("own-id"));
    }

    @Test
    void updateUser_rejectsInvalidRole() {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("a@a.com").enabled(true).role("not-a-role").build();

        assertThrows(BusinessException.class, () -> keycloakAdminService.updateUser("u1", request));
    }

    @Test
    void createUser_success_assignsRoleAndReturnsUser() {
        expectTokenFetch();
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .location(URI.create(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users/u2")));
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/roles/employee"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"r-emp","name":"employee"}
                        """, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users/u2/role-mappings/realm"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users/u2"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"u2","username":"newuser","email":"new@sistema.com",
                         "firstName":"New","lastName":"User","enabled":true,"createdTimestamp":1700000000000}
                        """, MediaType.APPLICATION_JSON));

        UserCreateRequest request = UserCreateRequest.builder()
                .username("newuser").email("new@sistema.com").password("password123")
                .firstName("New").lastName("User").role("employee").build();

        UserResponse result = keycloakAdminService.createUser(request);

        assertEquals("u2", result.getId());
        assertEquals("newuser", result.getUsername());
        assertEquals("employee", result.getRole());
        mockServer.verify();
    }

    @Test
    void updateUser_success_whenRoleUnchanged() {
        expectTokenFetch();
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users/u1"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"u1","username":"admin","email":"old@sistema.com",
                         "firstName":"Admin","lastName":"Sistema","enabled":true,"createdTimestamp":1700000000000}
                        """, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users/u1"))
                .andExpect(method(org.springframework.http.HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users/u1/role-mappings/realm"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":"r1","name":"admin"}]
                        """, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users/u1"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"u1","username":"admin","email":"updated@sistema.com",
                         "firstName":"Admin","lastName":"Sistema","enabled":true,"createdTimestamp":1700000000000}
                        """, MediaType.APPLICATION_JSON));

        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("updated@sistema.com").firstName("Admin").lastName("Sistema")
                .enabled(true).role("admin").build();

        UserResponse result = keycloakAdminService.updateUser("u1", request);

        assertEquals("updated@sistema.com", result.getEmail());
        assertEquals("admin", result.getRole());
        mockServer.verify();
    }

    @Test
    void deleteUser_success_whenNotSelf() {
        expectTokenFetch();
        mockServer.expect(requestTo(AUTH_SERVER_URL + "/admin/realms/sistema-gestion/users/u1"))
                .andExpect(method(org.springframework.http.HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        assertDoesNotThrow(() -> keycloakAdminService.deleteUser("u1"));
        mockServer.verify();
    }
}
