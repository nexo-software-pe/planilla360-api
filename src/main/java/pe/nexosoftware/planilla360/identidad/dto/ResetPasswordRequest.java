package pe.nexosoftware.planilla360.identidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
        @NotBlank @Pattern(regexp = PoliticaPassword.PATRON, message = PoliticaPassword.MENSAJE)
                String passwordNueva) {
}
