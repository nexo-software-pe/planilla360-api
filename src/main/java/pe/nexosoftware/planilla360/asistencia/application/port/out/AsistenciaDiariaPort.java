package pe.nexosoftware.planilla360.asistencia.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.domain.model.AsistenciaDiaria;

public interface AsistenciaDiariaPort {

    record Registro(UUID empleadoId, AsistenciaDiaria dia) {
    }

    /** Guarda el día consolidado; si ya existía uno para ese empleado y fecha, lo reemplaza. */
    void reemplazar(UUID empleadoId, AsistenciaDiaria dia);

    List<AsistenciaDiaria> delEmpleado(UUID empleadoId, LocalDate desde, LocalDate hasta);

    List<Registro> delPeriodo(LocalDate desde, LocalDate hasta);
}
