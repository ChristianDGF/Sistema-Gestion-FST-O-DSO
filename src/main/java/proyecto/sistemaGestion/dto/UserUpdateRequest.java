package proyecto.sistemaGestion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserUpdateRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no es válido")
    private String email;

    private String firstName;

    private String lastName;

    @NotNull(message = "El estado enabled es obligatorio")
    private Boolean enabled;

    @NotBlank(message = "El rol es obligatorio")
    private String role;
}
