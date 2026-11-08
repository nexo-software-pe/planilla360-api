package pe.nexosoftware.planilla360.asistencia.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lo que asistencia necesita saber de los empleados (los datos viven en el módulo de empleados). */
public interface ConsultaEmpleadoPort {

    record EmpleadoInfo(UUID id, String nombreCompleto, UUID jefeId) {
    }

    Optional<EmpleadoInfo> deUsuario(UUID usuarioId);

    Optional<EmpleadoInfo> buscar(UUID empleadoId);

    /** Empleados vigentes (ingresados y no cesados) en la fecha. */
    List<EmpleadoInfo> vigentesEn(LocalDate fecha);
}
