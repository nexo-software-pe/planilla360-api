package pe.nexosoftware.planilla360.vacaciones.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.domain.model.PeriodoVacacional;

public interface PeriodoVacacionalPort {

    record Guardado(UUID empleadoId, PeriodoVacacional periodo) {
    }

    /** Periodos del empleado, del más antiguo al más reciente. */
    List<PeriodoVacacional> delEmpleado(UUID empleadoId);

    Optional<Guardado> buscar(UUID periodoId);

    /** Crea el periodo o actualiza sus días gozados y vendidos. */
    void guardar(UUID empleadoId, PeriodoVacacional periodo);
}
