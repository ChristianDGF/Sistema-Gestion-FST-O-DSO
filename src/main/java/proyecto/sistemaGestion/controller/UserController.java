package proyecto.sistemaGestion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import proyecto.sistemaGestion.dto.PageResponse;
import proyecto.sistemaGestion.dto.UserCreateRequest;
import proyecto.sistemaGestion.dto.UserResponse;
import proyecto.sistemaGestion.dto.UserUpdateRequest;
import proyecto.sistemaGestion.service.KeycloakAdminService;

@RestController
@RequestMapping(value = "/api/v1/users", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "API para la gestión de usuarios, roles y permisos (proxy de la Admin API de Keycloak)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAuthority('SCOPE_user:manage')")
public class UserController {

    private final KeycloakAdminService keycloakAdminService;

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Obtiene una lista paginada de usuarios de Keycloak con su rol asignado")
    public ResponseEntity<PageResponse<UserResponse>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(keycloakAdminService.listUsers(page, size, search));
    }

    @PostMapping
    @Operation(summary = "Crear usuario", description = "Crea un usuario en Keycloak con contraseña inicial y un rol (admin|employee)")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(keycloakAdminService.createUser(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza perfil, estado (enabled) y rol de un usuario")
    public ResponseEntity<UserResponse> update(@PathVariable String id, @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(keycloakAdminService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario de Keycloak. No permite auto-eliminarse.")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        keycloakAdminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
