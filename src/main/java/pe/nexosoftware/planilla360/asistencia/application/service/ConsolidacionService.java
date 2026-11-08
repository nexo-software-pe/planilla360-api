package pe.nexosoftware.planilla360.asistencia.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.asistencia.application.port.in.ConsolidarAsistenciaUseCase;
import pe.nexosoftware.planilla360.asistencia.application.port.out.AsistenciaDiariaPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.ConsultaEmpleadoPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.ConsultaEmpleadoPort.EmpleadoInfo;
import pe.nexosoftware.planilla360.asistencia.application.port.out.ConsultaFeriadoPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.DiaJustificadoPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.HorarioPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.MarcacionPort;
import pe.nexosoftware.planilla360.asistencia.domain.model.AsistenciaDiaria;
import pe.nexosoftware.planilla360.asistencia.domain.model.ContextoDia;
import pe.nexosoftware.planilla360.asistencia.domain.model.Horario;
import pe.nexosoftware.planilla360.asistencia.domain.model.Marcacion;
import pe.nexosoftware.planilla360.asistencia.domain.service.ConsolidadorAsistencia;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;

/**
 * Consolidación diaria: una consulta por tipo de dato (no una por empleado) y el dominio decide el
 * resultado de cada uno.
 */
@Service
public class ConsolidacionService implements ConsolidarAsistenciaUseCase {

    private final ConsultaEmpleadoPort empleados;
    private final HorarioPort horarios;
    private final MarcacionPort marcaciones;
    private final DiaJustificadoPort justificados;
    private final ConsultaFeriadoPort feriados;
    private final AsistenciaDiariaPort asistencias;
    private final Clock reloj;
    private final ConsolidadorAsistencia consolidador = new ConsolidadorAsistencia();

    public ConsolidacionService(
            ConsultaEmpleadoPort empleados,
            HorarioPort horarios,
            MarcacionPort marcaciones,
            DiaJustificadoPort justificados,
            ConsultaFeriadoPort feriados,
            AsistenciaDiariaPort asistencias,
            Clock reloj) {
        this.empleados = empleados;
        this.horarios = horarios;
        this.marcaciones = marcaciones;
        this.justificados = justificados;
        this.feriados = feriados;
        this.asistencias = asistencias;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public int consolidar(LocalDate fecha) {
        if (!fecha.isBefore(HoraLima.hoy(reloj))) {
            throw new ReglaDeNegocioException("Solo se puede consolidar un día ya terminado.");
        }
        List<EmpleadoInfo> vigentes = empleados.vigentesEn(fecha);
        Map<UUID, Horario> horariosVigentes = horarios.vigentesEn(fecha);
        Map<UUID, List<Marcacion>> marcacionesDelDia =
                marcaciones.entreDeTodos(HoraLima.inicioDe(fecha), HoraLima.inicioDe(fecha.plusDays(1)));
        Set<UUID> conDiaJustificado = justificados.empleadosJustificadosEn(fecha);
        boolean feriado = feriados.esFeriado(fecha);

        int procesados = 0;
        for (EmpleadoInfo empleado : vigentes) {
            Horario horario = horariosVigentes.get(empleado.id());
            if (horario == null) {
                continue;
            }
            ContextoDia contexto = new ContextoDia(
                    fecha,
                    horario,
                    feriado,
                    conDiaJustificado.contains(empleado.id()),
                    marcacionesDelDia.getOrDefault(empleado.id(), List.of()));
            AsistenciaDiaria dia = consolidador.consolidar(contexto);
            asistencias.reemplazar(empleado.id(), dia);
            procesados++;
        }
        return procesados;
    }
}
