package pe.nexosoftware.planilla360.asistencia.application.model;

import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;

/** Quién ejecuta un caso de uso: la cuenta de usuario y su rol. */
public record Actor(UUID usuarioId, Rol rol) {

    /** RRHH y el administrador de la empresa gestionan al personal completo. */
    public boolean gestionaPersonal() {
        return rol == Rol.RRHH || rol == Rol.ADMIN_EMPRESA;
    }
}
