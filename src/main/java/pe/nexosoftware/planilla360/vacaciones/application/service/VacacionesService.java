package pe.nexosoftware.planilla360.vacaciones.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.excepcion.AccesoProhibidoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.vacaciones.VacacionesAprobadas;
import pe.nexosoftware.planilla360.vacaciones.application.model.Actor;
import pe.nexosoftware.planilla360.vacaciones.application.port.in.GestionarVacacionesUseCase;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.ConsultaEmpleadoPort;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.ConsultaEmpleadoPort.EmpleadoInfo;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.PeriodoVacacionalPort;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.PeriodoVacacionalPort.Guardado;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.PublicadorEventosPort;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.RegimenEmpresaPort;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.SolicitudVacacionesPort;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud;
import pe.nexosoftware.planilla360.vacaciones.domain.model.PeriodoVacacional;
import pe.nexosoftware.planilla360.vacaciones.domain.model.PoliticaVacaciones;
import pe.nexosoftware.planilla360.vacaciones.domain.model.SolicitudVacaciones;
import pe.nexosoftware.planilla360.vacaciones.domain.service.CalculadoraPeriodos;

/**
 * Casos de uso de vacaciones. Los periodos se crean la primera vez que se consultan (al cumplirse cada
 * año de servicio) y los días de las solicitudes activas se reservan sin tocar el saldo del periodo
 * hasta que el goce termina.
 */
@Service
@Transactional
public class VacacionesService implements GestionarVacacionesUseCase {

    private final SolicitudVacacionesPort solicitudes;
    private final PeriodoVacacionalPort periodos;
    private final ConsultaEmpleadoPort empleados;
    private final RegimenEmpresaPort regimen;
    private final PublicadorEventosPort eventos;
    private final Clock reloj;
    private final AccesoAEmpleados acceso;
    private final CalculadoraPeriodos calculadora = new CalculadoraPeriodos();

    public VacacionesService(
            SolicitudVacacionesPort solicitudes,
            PeriodoVacacionalPort periodos,
            ConsultaEmpleadoPort empleados,
            RegimenEmpresaPort regimen,
            PublicadorEventosPort eventos,
            Clock reloj) {
        this.solicitudes = solicitudes;
        this.periodos = periodos;
        this.empleados = empleados;
        this.regimen = regimen;
        this.eventos = eventos;
        this.reloj = reloj;
        this.acceso = new AccesoAEmpleados(empleados);
    }

    @Override
    public SaldoVacaciones miSaldo(UUID usuarioId) {
        EmpleadoInfo empleado = acceso.empleadoDe(new Actor(usuarioId, null));
        return saldoDe(empleado);
    }

    @Override
    public SolicitudVacaciones solicitar(Actor actor, LocalDate desde, LocalDate hasta) {
        EmpleadoInfo empleado = acceso.empleadoDe(actor);
        LocalDate hoy = HoraLima.hoy(reloj);
        for (SolicitudVacaciones activa : solicitudes.activasDelEmpleado(empleado.id())) {
            if (activa.seSolapaCon(desde, hasta)) {
                throw new ConflictoException(
                        "Ya tienes una solicitud de vacaciones del " + activa.desde() + " al " + activa.hasta() + ".");
            }
        }
        List<DetallePeriodo> detalle = detallar(empleado, hoy);
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        DetallePeriodo elegido = detalle.stream()
                .filter(p -> p.diasDisponibles() >= dias)
                .findFirst()
                .or(() -> detalle.stream().filter(p -> p.diasDisponibles() > 0).findFirst())
                .orElseThrow(() -> new ReglaDeNegocioException("No tienes días de vacaciones disponibles."));

        SolicitudVacaciones solicitud = SolicitudVacaciones.solicitar(
                empleado.id(), elegido.periodoId(), desde, hasta, elegido.diasDisponibles(), hoy);
        return solicitudes.guardar(solicitud);
    }

    @Override
    public SolicitudVacaciones aprobarPorJefe(Actor actor, UUID solicitudId) {
        SolicitudVacaciones solicitud = buscar(solicitudId);
        EmpleadoInfo solicitante = acceso.buscar(solicitud.empleadoId());
        acceso.exigirQueNoSeaElMismo(actor, solicitante.id());
        boolean rrhhSuple = actor.gestionaPersonal() && solicitante.jefeId() == null;
        if (!rrhhSuple) {
            exigirJefeDirecto(actor, solicitante);
        }
        solicitud.aprobarPorJefe();
        return solicitudes.guardar(solicitud);
    }

    @Override
    public SolicitudVacaciones aprobarPorRrhh(Actor actor, UUID solicitudId) {
        SolicitudVacaciones solicitud = buscar(solicitudId);
        acceso.exigirQueNoSeaElMismo(actor, solicitud.empleadoId());
        if (!actor.gestionaPersonal()) {
            throw new AccesoProhibidoException(
                    "Solo RRHH puede dar la aprobación final.");
        }
        solicitud.aprobarPorRrhh();
        SolicitudVacaciones guardada = solicitudes.guardar(solicitud);
        eventos.publicar(new VacacionesAprobadas(
                guardada.id(),
                guardada.empleadoId(),
                TenantContext.requerido(),
                guardada.desde(),
                guardada.hasta(),
                guardada.dias()));
        return guardada;
    }

    @Override
    public SolicitudVacaciones rechazar(Actor actor, UUID solicitudId, String motivo) {
        SolicitudVacaciones solicitud = buscar(solicitudId);
        EmpleadoInfo solicitante = acceso.buscar(solicitud.empleadoId());
        acceso.exigirQueNoSeaElMismo(actor, solicitante.id());
        acceso.exigirPersonalOJefe(actor, solicitante);
        solicitud.rechazar(motivo);
        return solicitudes.guardar(solicitud);
    }

    @Override
    public SolicitudVacaciones cancelar(Actor actor, UUID solicitudId) {
        SolicitudVacaciones solicitud = buscar(solicitudId);
        boolean esDelActor = empleados.deUsuario(actor.usuarioId())
                .map(EmpleadoInfo::id)
                .filter(solicitud.empleadoId()::equals)
                .isPresent();
        if (!esDelActor && !actor.gestionaPersonal()) {
            throw new AccesoProhibidoException(
                    "Solo el dueño de la solicitud o RRHH pueden cancelarla.");
        }
        solicitud.cancelar();
        return solicitudes.guardar(solicitud);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudVacaciones> listar(Actor actor, EstadoSolicitud estado, UUID empleadoId) {
        if (actor.gestionaPersonal()) {
            return solicitudes.listar(empleadoId == null ? null : List.of(empleadoId), estado);
        }
        EmpleadoInfo yo = acceso.empleadoDe(actor);
        Set<UUID> visibles = new HashSet<>();
        visibles.add(yo.id());
        empleados.vigentesEn(HoraLima.hoy(reloj)).stream()
                .filter(e -> yo.id().equals(e.jefeId()))
                .forEach(e -> visibles.add(e.id()));
        if (empleadoId != null) {
            visibles.retainAll(Set.of(empleadoId));
        }
        return solicitudes.listar(visibles, estado);
    }

    @Override
    public void venderDias(Actor actor, UUID periodoId, int dias) {
        if (!actor.gestionaPersonal()) {
            throw new AccesoProhibidoException(
                    "Solo RRHH puede registrar la venta de vacaciones.");
        }
        Guardado guardado = periodos.buscar(periodoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el periodo vacacional."));
        int reservados = solicitudes.activasDelEmpleado(guardado.empleadoId()).stream()
                .filter(s -> s.periodoId().equals(periodoId))
                .mapToInt(SolicitudVacaciones::dias)
                .sum();
        PeriodoVacacional periodo = guardado.periodo();
        if (dias > periodo.saldo() - reservados) {
            throw new ReglaDeNegocioException(
                    "Solo se pueden vender los días libres (hay " + reservados + " reservados por solicitudes activas).");
        }
        periodo.vender(dias, regimen.politicaDeLaEmpresa());
        periodos.guardar(guardado.empleadoId(), periodo);
    }

    private void exigirJefeDirecto(Actor actor, EmpleadoInfo solicitante) {
        boolean esSuJefe = empleados.deUsuario(actor.usuarioId())
                .map(EmpleadoInfo::id)
                .filter(id -> id.equals(solicitante.jefeId()))
                .isPresent();
        if (!esSuJefe) {
            throw new AccesoProhibidoException(
                    "Solo el jefe directo puede dar esta aprobación.");
        }
    }

    private SaldoVacaciones saldoDe(EmpleadoInfo empleado) {
        List<DetallePeriodo> detalle = detallar(empleado, HoraLima.hoy(reloj));
        return new SaldoVacaciones(detalle.stream().mapToInt(DetallePeriodo::diasDisponibles).sum(), detalle);
    }

    /** Crea los periodos que faltan y calcula, por periodo, los días reservados y disponibles. */
    private List<DetallePeriodo> detallar(EmpleadoInfo empleado, LocalDate hoy) {
        PoliticaVacaciones politica = regimen.politicaDeLaEmpresa();
        List<PeriodoVacacional> existentes = new ArrayList<>(periodos.delEmpleado(empleado.id()));
        Set<LocalDate> inicios = new HashSet<>();
        existentes.forEach(p -> inicios.add(p.desde()));
        calculadora.periodosCumplidos(empleado.fechaIngreso(), empleado.fechaCese(), hoy, politica).stream()
                .filter(calculado -> !inicios.contains(calculado.desde()))
                .forEach(calculado -> {
                    PeriodoVacacional nuevo = new PeriodoVacacional(
                            UUID.randomUUID(), calculado.desde(), calculado.hasta(), calculado.diasGanados(), 0, 0);
                    periodos.guardar(empleado.id(), nuevo);
                    existentes.add(nuevo);
                });
        existentes.sort((a, b) -> a.desde().compareTo(b.desde()));

        List<SolicitudVacaciones> activas = solicitudes.activasDelEmpleado(empleado.id());
        List<DetallePeriodo> detalle = new ArrayList<>();
        for (PeriodoVacacional periodo : existentes) {
            int reservados = activas.stream()
                    .filter(s -> s.periodoId().equals(periodo.id()))
                    .mapToInt(SolicitudVacaciones::dias)
                    .sum();
            detalle.add(new DetallePeriodo(
                    periodo.id(),
                    periodo.desde(),
                    periodo.hasta(),
                    periodo.diasGanados(),
                    periodo.diasGozados(),
                    periodo.diasVendidos(),
                    reservados,
                    Math.max(0, periodo.saldo() - reservados)));
        }
        return detalle;
    }

    private SolicitudVacaciones buscar(UUID id) {
        return solicitudes.buscar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la solicitud de vacaciones."));
    }
}
