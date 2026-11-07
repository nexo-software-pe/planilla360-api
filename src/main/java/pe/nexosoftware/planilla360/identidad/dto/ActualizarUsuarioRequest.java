package pe.nexosoftware.planilla360.identidad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;

public record ActualizarUsuarioRequest(
        @NotBlank @Size(max = 150) String nombre, @NotNull Rol rol, UUID empleadoId) {
}
