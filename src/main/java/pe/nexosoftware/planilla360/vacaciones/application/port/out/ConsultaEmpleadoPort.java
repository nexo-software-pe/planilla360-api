package pe.nexosoftware.planilla360.vacaciones.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lo que vacaciones necesita saber de los empleados (los datos viven en el módulo de empleados). */
public interface ConsultaEmpleadoPort {

    record EmpleadoInfo(
            UUID id, String nombreCompleto, UUID jefeId, LocalDate fechaIngreso, LocalDate fechaCese) {
    }

    Optional<EmpleadoInfo> deUsuario(UUID usuarioId);

    Optional<EmpleadoInfo> buscar(UUID empleadoId);

    List<EmpleadoInfo> vigentesEn(LocalDate fecha);
}
