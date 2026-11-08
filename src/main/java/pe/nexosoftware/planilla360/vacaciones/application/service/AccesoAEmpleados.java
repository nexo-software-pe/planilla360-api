package pe.nexosoftware.planilla360.vacaciones.application.service;

import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.application.model.Actor;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.ConsultaEmpleadoPort;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.ConsultaEmpleadoPort.EmpleadoInfo;
import pe.nexosoftware.planilla360.compartido.excepcion.AccesoProhibidoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;

/** Reglas de acceso comunes: quién es el actor y si puede intervenir sobre un empleado. */
final class AccesoAEmpleados {

    private final ConsultaEmpleadoPort empleados;

    AccesoAEmpleados(ConsultaEmpleadoPort empleados) {
        this.empleados = empleados;
    }

    EmpleadoInfo empleadoDe(Actor actor) {
        return empleados.deUsuario(actor.usuarioId())
                .orElseThrow(() -> new ReglaDeNegocioException("Tu cuenta no está vinculada a un empleado."));
    }

    EmpleadoInfo buscar(UUID empleadoId) {
        return empleados.buscar(empleadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el empleado solicitado."));
    }

    /** Nadie resuelve sus propias solicitudes, ni siquiera RRHH. */
    void exigirQueNoSeaElMismo(Actor actor, UUID empleadoDeLaSolicitud) {
        if (empleados.deUsuario(actor.usuarioId()).map(EmpleadoInfo::id).filter(empleadoDeLaSolicitud::equals).isPresent()) {
            throw new AccesoProhibidoException("No puedes resolver tus propias solicitudes.");
        }
    }

    /** RRHH, el administrador de la empresa o el jefe directo del empleado. */
    void exigirPersonalOJefe(Actor actor, EmpleadoInfo solicitante) {
        boolean esSuJefe = empleados.deUsuario(actor.usuarioId())
                .map(EmpleadoInfo::id)
                .filter(id -> id.equals(solicitante.jefeId()))
                .isPresent();
        if (!actor.gestionaPersonal() && !esSuJefe) {
            throw new AccesoProhibidoException("Solo RRHH o el jefe directo pueden resolver esta solicitud.");
        }
    }
}
