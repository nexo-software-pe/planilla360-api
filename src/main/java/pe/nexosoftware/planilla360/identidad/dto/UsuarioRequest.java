package pe.nexosoftware.planilla360.identidad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;

/** Alta de usuario. {@code tenantId} solo lo usa el administrador de la plataforma. */
public record UsuarioRequest(
        @NotBlank @Email @Size(max = 160) String email,
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Pattern(regexp = PoliticaPassword.PATRON, message = PoliticaPassword.MENSAJE) String password,
        @NotNull Rol rol,
        UUID empleadoId,
        UUID tenantId) {
}
