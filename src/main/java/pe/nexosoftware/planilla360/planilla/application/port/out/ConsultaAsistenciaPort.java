package pe.nexosoftware.planilla360.planilla.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.application.model.NovedadesAsistencia;

/** Asistencia del mes que la planilla necesita (la provee el módulo de asistencia). */
public interface ConsultaAsistenciaPort {

    Map<UUID, NovedadesAsistencia> novedadesDelMes(Periodo periodo);

    /** Horas extra aprobadas por trabajador, una entrada por día. */
    Map<UUID, List<BigDecimal>> horasExtraAprobadasDelMes(Periodo periodo);

    /** Minutos efectivos por día del horario de cada trabajador en la fecha (sin horario: no aparece). */
    Map<UUID, Integer> minutosDeHorarioEn(LocalDate fecha);

    boolean mesCompleto(Periodo periodo);
}
