package pe.nexosoftware.planilla360.identidad;

import java.util.Optional;
import java.util.UUID;

/** API pública del módulo de identidad. */
public interface IdentidadApi {

    /** Empleado vinculado a la cuenta de usuario, si la cuenta pertenece a un empleado. */
    Optional<UUID> empleadoIdDe(UUID usuarioId);
}
