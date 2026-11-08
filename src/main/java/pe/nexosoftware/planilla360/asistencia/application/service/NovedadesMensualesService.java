package pe.nexosoftware.planilla360.asistencia.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.asistencia.AsistenciaApi;
import pe.nexosoftware.planilla360.asistencia.application.port.out.AsistenciaDiariaPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.AsistenciaDiariaPort.Registro;
import pe.nexosoftware.planilla360.asistencia.application.port.out.HoraExtraPort;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoAsistencia;
import pe.nexosoftware.planilla360.asistencia.domain.model.HoraExtra;

/** Respuestas de asistencia para otros módulos (la planilla las usa para calcular y validar el mes). */
@Service
@Transactional(readOnly = true)
public class NovedadesMensualesService implements AsistenciaApi {

    private final AsistenciaDiariaPort asistencias;
    private final HoraExtraPort horasExtra;

    public NovedadesMensualesService(AsistenciaDiariaPort asistencias, HoraExtraPort horasExtra) {
        this.asistencias = asistencias;
        this.horasExtra = horasExtra;
    }

    @Override
    public Map<UUID, NovedadesDelMes> novedadesDelMes(YearMonth mes) {
        Map<UUID, int[]> acumulado = new HashMap<>();
        for (Registro registro : asistencias.delPeriodo(mes.atDay(1), mes.atEndOfMonth())) {
            int[] totales = acumulado.computeIfAbsent(registro.empleadoId(), id -> new int[2]);
            totales[0] += registro.dia().minutosTardanza();
            if (registro.dia().estado() == EstadoAsistencia.FALTA) {
                totales[1]++;
            }
        }
        Map<UUID, NovedadesDelMes> novedades = new HashMap<>();
        acumulado.forEach((id, t) -> novedades.put(id, new NovedadesDelMes(id, t[0], t[1])));
        return novedades;
    }

    @Override
    public Map<UUID, List<BigDecimal>> horasExtraAprobadasDelMes(YearMonth mes) {
        Map<UUID, Map<LocalDate, BigDecimal>> porDia = new HashMap<>();
        for (HoraExtra aprobada : horasExtra.aprobadasEntre(mes.atDay(1), mes.atEndOfMonth())) {
            porDia.computeIfAbsent(aprobada.empleadoId(), id -> new TreeMap<>())
                    .merge(aprobada.fecha(), aprobada.horas(), BigDecimal::add);
        }
        Map<UUID, List<BigDecimal>> resultado = new HashMap<>();
        porDia.forEach((id, dias) -> resultado.put(id, new ArrayList<>(dias.values())));
        return resultado;
    }

    @Override
    public boolean mesCompleto(YearMonth mes) {
        List<Registro> registros = asistencias.delPeriodo(mes.atDay(1), mes.atEndOfMonth());
        boolean ultimoDiaConsolidado =
                registros.stream().anyMatch(r -> r.dia().fecha().equals(mes.atEndOfMonth()));
        boolean sinIncompletos = registros.stream().noneMatch(r -> r.dia().estado() == EstadoAsistencia.INCOMPLETA);
        return ultimoDiaConsolidado && sinIncompletos;
    }
}
