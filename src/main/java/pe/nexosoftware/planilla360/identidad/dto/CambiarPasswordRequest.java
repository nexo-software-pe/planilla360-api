package pe.nexosoftware.planilla360.identidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotBlank @Size(max = 72) String passwordActual,
        @NotBlank @Pattern(regexp = PoliticaPassword.PATRON, message = PoliticaPassword.MENSAJE)
                String passwordNueva) {
}
