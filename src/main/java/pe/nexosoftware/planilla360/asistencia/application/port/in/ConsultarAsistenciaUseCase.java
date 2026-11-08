package pe.nexosoftware.planilla360.asistencia.application.port.in;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.domain.model.AsistenciaDiaria;

public interface ConsultarAsistenciaUseCase {

    record ResumenMensual(
            UUID empleadoId,
            String nombreCompleto,
            int diasAsistidos,
            int tardanzas,
            int minutosTardanza,
            int faltas,
            int diasJustificados,
            int minutosExtra) {
    }

    List<AsistenciaDiaria> deMiMes(UUID usuarioId, YearMonth mes);

    List<AsistenciaDiaria> deEmpleado(UUID empleadoId, YearMonth mes);

    /** Totales del mes por empleado, ordenados por nombre. */
    List<ResumenMensual> resumen(YearMonth mes);
}
