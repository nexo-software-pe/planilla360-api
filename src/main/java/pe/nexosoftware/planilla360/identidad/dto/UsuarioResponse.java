package pe.nexosoftware.planilla360.identidad.dto;

import java.time.Instant;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;

public record UsuarioResponse(
        UUID id,
        UUID tenantId,
        String email,
        String nombre,
        Rol rol,
        UUID empleadoId,
        boolean activo,
        Instant ultimoAcceso,
        Instant creadoEn) {
}
