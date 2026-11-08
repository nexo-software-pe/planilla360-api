package pe.nexosoftware.planilla360.asistencia.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.application.port.in.JustificarDiasUseCase.Motivo;

public interface DiaJustificadoPort {

    /** Registra los días como justificados; los que ya lo estaban se respetan. */
    void justificar(UUID empleadoId, List<LocalDate> fechas, Motivo motivo, UUID origenId);

    /** Empleados con el día justificado (vacaciones o permiso aprobado). */
    Set<UUID> empleadosJustificadosEn(LocalDate fecha);
}
