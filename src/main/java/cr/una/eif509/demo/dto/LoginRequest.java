package cr.una.eif509.demo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @Schema(description = "Correo del usuario", example = "admin@demo.cr")
        @NotBlank(message = "Este campo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        String correo,

        @Schema(description = "Clave del usuario", example = "admin123")
        @NotBlank(message = "Este campo es obligatorio.")
        String clave) {
}
