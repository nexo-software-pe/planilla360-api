package pe.nexosoftware.planilla360.asistencia.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.asistencia.application.port.in.ConsultarAsistenciaUseCase;
import pe.nexosoftware.planilla360.asistencia.application.port.in.RegistrarMarcacionUseCase;
import pe.nexosoftware.planilla360.asistencia.application.port.out.AsistenciaDiariaPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.AsistenciaDiariaPort.Registro;
import pe.nexosoftware.planilla360.asistencia.application.port.out.ConsultaEmpleadoPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.ConsultaEmpleadoPort.EmpleadoInfo;
import pe.nexosoftware.planilla360.asistencia.application.port.out.MarcacionPort;
import pe.nexosoftware.planilla360.asistencia.domain.model.AsistenciaDiaria;
import pe.nexosoftware.planilla360.asistencia.domain.model.Marcacion;
import pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion;
import pe.nexosoftware.planilla360.asistencia.domain.service.PoliticaMarcacion;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;

/** Marcaciones del empleado y consulta de la asistencia consolidada. */
@Service
@Transactional(readOnly = true)
public class AsistenciaService implements RegistrarMarcacionUseCase, ConsultarAsistenciaUseCase {

    private final MarcacionPort marcaciones;
    private final AsistenciaDiariaPort asistencias;
    private final ConsultaEmpleadoPort empleados;
    private final Clock reloj;
    private final PoliticaMarcacion politica = new PoliticaMarcacion();

    public AsistenciaService(
            MarcacionPort marcaciones,
            AsistenciaDiariaPort asistencias,
            ConsultaEmpleadoPort empleados,
            Clock reloj) {
        this.marcaciones = marcaciones;
        this.asistencias = asistencias;
        this.empleados = empleados;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public MarcacionRegistrada registrar(UUID usuarioId) {
        EmpleadoInfo empleado = empleadoDe(usuarioId);
        Instant ahora = reloj.instant();
        LocalDate hoy = HoraLima.hoy(reloj);
        List<Marcacion> delDia = marcaciones.entre(
                empleado.id(), HoraLima.inicioDe(hoy), HoraLima.inicioDe(hoy.plusDays(1)));

        TipoMarcacion tipo = politica.siguienteTipo(delDia, ahora);
        marcaciones.guardar(empleado.id(), new Marcacion(ahora, tipo));
        return new MarcacionRegistrada(tipo, ahora);
    }

    @Override
    public List<AsistenciaDiaria> deMiMes(UUID usuarioId, YearMonth mes) {
        return asistencias.delEmpleado(empleadoDe(usuarioId).id(), mes.atDay(1), mes.atEndOfMonth());
    }

    @Override
    public List<AsistenciaDiaria> deEmpleado(UUID empleadoId, YearMonth mes) {
        EmpleadoInfo empleado = empleados.buscar(empleadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el empleado solicitado."));
        return asistencias.delEmpleado(empleado.id(), mes.atDay(1), mes.atEndOfMonth());
    }

    @Override
    public List<ResumenMensual> resumen(YearMonth mes) {
        Map<UUID, String> nombres = new HashMap<>();
        empleados.vigentesEn(mes.atDay(1)).forEach(e -> nombres.put(e.id(), e.nombreCompleto()));
        empleados.vigentesEn(mes.atEndOfMonth()).forEach(e -> nombres.put(e.id(), e.nombreCompleto()));

        Map<UUID, List<AsistenciaDiaria>> porEmpleado = new HashMap<>();
        for (Registro registro : asistencias.delPeriodo(mes.atDay(1), mes.atEndOfMonth())) {
            porEmpleado.computeIfAbsent(registro.empleadoId(), id -> new ArrayList<>()).add(registro.dia());
        }
        return porEmpleado.entrySet().stream()
                .map(e -> totalizar(e.getKey(), nombres.getOrDefault(e.getKey(), e.getKey().toString()), e.getValue()))
                .sorted(Comparator.comparing(ResumenMensual::nombreCompleto))
                .toList();
    }

    private static ResumenMensual totalizar(UUID empleadoId, String nombre, List<AsistenciaDiaria> dias) {
        int asistidos = 0;
        int tardanzas = 0;
        int minutosTardanza = 0;
        int faltas = 0;
        int justificados = 0;
        int minutosExtra = 0;
        for (AsistenciaDiaria dia : dias) {
            minutosTardanza += dia.minutosTardanza();
            minutosExtra += dia.minutosExtra();
            switch (dia.estado()) {
                case PRESENTE -> asistidos++;
                case TARDANZA -> {
                    asistidos++;
                    tardanzas++;
                }
                case FALTA -> faltas++;
                case JUSTIFICADA -> justificados++;
                case INCOMPLETA, FERIADO, DESCANSO -> {
                    // No suman a asistencias, faltas ni justificaciones.
                }
            }
        }
        return new ResumenMensual(
                empleadoId, nombre, asistidos, tardanzas, minutosTardanza, faltas, justificados, minutosExtra);
    }

    private EmpleadoInfo empleadoDe(UUID usuarioId) {
        return empleados.deUsuario(usuarioId)
                .orElseThrow(() -> new ReglaDeNegocioException("Tu cuenta no está vinculada a un empleado."));
    }
}
