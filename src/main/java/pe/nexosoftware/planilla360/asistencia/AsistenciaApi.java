package pe.nexosoftware.planilla360.asistencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** API pública del módulo de asistencia. Las consultas se limitan a la empresa del contexto. */
public interface AsistenciaApi {

    /** Tardanzas y faltas injustificadas de un empleado en el mes. */
    record NovedadesDelMes(UUID empleadoId, int minutosTardanza, int faltasInjustificadas) {
    }

    /** Novedades de asistencia del mes por empleado (solo aparecen los que tienen días consolidados). */
    Map<UUID, NovedadesDelMes> novedadesDelMes(YearMonth mes);

    /** Horas extra aprobadas del mes por empleado: una entrada por día, con las horas de ese día sumadas. */
    Map<UUID, List<BigDecimal>> horasExtraAprobadasDelMes(YearMonth mes);

    /**
     * Minutos efectivos por día (salida - entrada - refrigerio) del horario que tenía cada empleado en la fecha. Los
     * empleados sin horario asignado no aparecen.
     */
    Map<UUID, Integer> minutosDeHorarioEn(LocalDate fecha);

    /** El mes está completo si ya se consolidó su último día y ningún día quedó con marcaciones incompletas. */
    boolean mesCompleto(YearMonth mes);
}
