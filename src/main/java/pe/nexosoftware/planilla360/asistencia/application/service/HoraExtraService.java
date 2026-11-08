package pe.nexosoftware.planilla360.asistencia.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.asistencia.application.model.Actor;
import pe.nexosoftware.planilla360.asistencia.application.port.in.GestionarHorasExtraUseCase;
import pe.nexosoftware.planilla360.asistencia.application.port.out.ConsultaEmpleadoPort;
import pe.nexosoftware.planilla360.asistencia.application.port.out.ConsultaEmpleadoPort.EmpleadoInfo;
import pe.nexosoftware.planilla360.asistencia.application.port.out.HoraExtraPort;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoHoraExtra;
import pe.nexosoftware.planilla360.asistencia.domain.model.HoraExtra;
import pe.nexosoftware.planilla360.compartido.excepcion.AccesoProhibidoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;

@Service
@Transactional(readOnly = true)
public class HoraExtraService implements GestionarHorasExtraUseCase {

    private final HoraExtraPort horasExtra;
    private final ConsultaEmpleadoPort empleados;
    private final Clock reloj;

    public HoraExtraService(HoraExtraPort horasExtra, ConsultaEmpleadoPort empleados, Clock reloj) {
        this.horasExtra = horasExtra;
        this.empleados = empleados;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public HoraExtra solicitar(Actor actor, UUID empleadoId, LocalDate fecha, BigDecimal horas, String motivo) {
        UUID paraQuien;
        if (actor.gestionaPersonal()) {
            if (empleadoId == null) {
                throw new ReglaDeNegocioException("Indique el empleado de la solicitud.");
            }
            paraQuien = empleados.buscar(empleadoId)
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el empleado solicitado."))
                    .id();
        } else {
            EmpleadoInfo yo = empleadoDe(actor);
            if (empleadoId != null && !empleadoId.equals(yo.id())) {
                throw new AccesoProhibidoException("Solo puedes solicitar horas extra para ti.");
            }
            paraQuien = yo.id();
        }
        return horasExtra.guardar(HoraExtra.solicitar(paraQuien, fecha, horas, motivo, HoraLima.hoy(reloj)));
    }

    @Override
    @Transactional
    public HoraExtra resolver(Actor actor, UUID id, boolean aprobar) {
        HoraExtra horaExtra = horasExtra.buscar(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la solicitud de horas extra."));
        EmpleadoInfo solicitante = empleados.buscar(horaExtra.empleadoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el empleado solicitado."));
        EmpleadoInfo yo = empleados.deUsuario(actor.usuarioId()).orElse(null);

        if (yo != null && yo.id().equals(solicitante.id())) {
            throw new AccesoProhibidoException("No puedes resolver tus propias solicitudes.");
        }
        boolean esSuJefe = yo != null && yo.id().equals(solicitante.jefeId());
        if (!actor.gestionaPersonal() && !esSuJefe) {
            throw new AccesoProhibidoException("Solo RRHH o el jefe directo pueden resolver esta solicitud.");
        }
        if (aprobar) {
            horaExtra.aprobar(actor.usuarioId());
        } else {
            horaExtra.rechazar(actor.usuarioId());
        }
        return horasExtra.guardar(horaExtra);
    }

    @Override
    public List<HoraExtra> listar(Actor actor, EstadoHoraExtra estado) {
        if (actor.gestionaPersonal()) {
            return horasExtra.listar(null, estado);
        }
        EmpleadoInfo yo = empleadoDe(actor);
        Set<UUID> visibles = new HashSet<>();
        visibles.add(yo.id());
        empleados.vigentesEn(HoraLima.hoy(reloj)).stream()
                .filter(e -> yo.id().equals(e.jefeId()))
                .forEach(e -> visibles.add(e.id()));
        return horasExtra.listar(visibles, estado);
    }

    private EmpleadoInfo empleadoDe(Actor actor) {
        return empleados.deUsuario(actor.usuarioId())
                .orElseThrow(() -> new ReglaDeNegocioException("Tu cuenta no está vinculada a un empleado."));
    }
}
