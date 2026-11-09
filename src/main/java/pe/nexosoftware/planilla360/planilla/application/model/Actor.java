package pe.nexosoftware.planilla360.planilla.application.model;

import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;

/** Quién ejecuta un caso de uso: la cuenta de usuario y su rol. */
public record Actor(UUID usuarioId, Rol rol) {

    /** Solo el administrador de la empresa puede anular una planilla. */
    public boolean puedeAnularPlanillas() {
        return rol == Rol.ADMIN_EMPRESA;
    }
}
