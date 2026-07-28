package proyecto.sistemaGestion.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.UserCreateRequest;
import proyecto.sistemaGestion.dto.UserResponse;
import proyecto.sistemaGestion.dto.UserUpdateRequest;
import proyecto.sistemaGestion.exception.BusinessException;
import proyecto.sistemaGestion.service.KeycloakAdminService;

import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class UserControllerApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private KeycloakAdminService keycloakAdminService;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private UserResponse createResponse(String id, String username, String role) {
        return UserResponse.builder()
                .id(id).username(username).email(username + "@sistema.com")
                .firstName("Test").lastName("User").enabled(true).role(role)
                .createdTimestamp(1700000000000L).build();
    }

    @Test
    @WithMockUser(authorities = "SCOPE_user:manage")
    void findAll_shouldReturn200() throws Exception {
        when(keycloakAdminService.listUsers(anyInt(), anyInt(), any()))
                .thenReturn(PageResponse.<UserResponse>builder()
                        .content(List.of(createResponse("u1", "admin", "admin")))
                        .page(0).size(20).totalElements(1).totalPages(1).last(true).build());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("admin"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_user:view")
    void findAll_shouldReturn403_whenInsufficientPermission() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_user:manage")
    void create_shouldReturn201_whenValid() throws Exception {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("newuser").email("new@sistema.com").password("password123").role("employee").build();

        when(keycloakAdminService.createUser(any(UserCreateRequest.class)))
                .thenReturn(createResponse("u2", "newuser", "employee"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_user:manage")
    void create_shouldReturn400_whenInvalidData() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","email":"not-an-email","password":"123","role":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_user:manage")
    void create_shouldReturn400_whenRoleInvalid() throws Exception {
        UserCreateRequest request = UserCreateRequest.builder()
                .username("newuser").email("new@sistema.com").password("password123").role("superadmin").build();

        when(keycloakAdminService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new BusinessException("Rol inválido: superadmin"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_user:manage")
    void update_shouldReturn200_whenValid() throws Exception {
        UserUpdateRequest request = UserUpdateRequest.builder()
                .email("updated@sistema.com").enabled(true).role("admin").build();

        when(keycloakAdminService.updateUser(eq("u1"), any(UserUpdateRequest.class)))
                .thenReturn(createResponse("u1", "admin", "admin"));

        mockMvc.perform(put("/api/v1/users/u1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("admin"));
    }

    @Test
    @WithMockUser(authorities = "SCOPE_user:manage")
    void delete_shouldReturn204_whenDeleted() throws Exception {
        doNothing().when(keycloakAdminService).deleteUser("u1");

        mockMvc.perform(delete("/api/v1/users/u1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(authorities = "SCOPE_user:manage")
    void delete_shouldReturn400_whenSelfDelete() throws Exception {
        doThrow(new BusinessException("No puede eliminar su propio usuario"))
                .when(keycloakAdminService).deleteUser("own-id");

        mockMvc.perform(delete("/api/v1/users/own-id"))
                .andExpect(status().isBadRequest());
    }
}
