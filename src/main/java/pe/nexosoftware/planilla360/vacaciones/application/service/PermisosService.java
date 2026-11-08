package pe.nexosoftware.planilla360.vacaciones.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.excepcion.AccesoProhibidoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.vacaciones.PermisoAprobado;
import pe.nexosoftware.planilla360.vacaciones.application.model.Actor;
import pe.nexosoftware.planilla360.vacaciones.application.port.in.GestionarPermisosUseCase;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.ConsultaEmpleadoPort;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.PermisoPort;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.PublicadorEventosPort;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoPermiso;
import pe.nexosoftware.planilla360.vacaciones.domain.model.Permiso;
import pe.nexosoftware.planilla360.vacaciones.domain.model.TipoPermiso;

/** Permisos y licencias: los resuelve RRHH; al aprobarse, sus días quedan justificados en asistencia. */
@Service
@Transactional
public class PermisosService implements GestionarPermisosUseCase {

    private final PermisoPort permisos;
    private final PublicadorEventosPort eventos;
    private final AccesoAEmpleados acceso;

    public PermisosService(PermisoPort permisos, ConsultaEmpleadoPort empleados, PublicadorEventosPort eventos) {
        this.permisos = permisos;
        this.eventos = eventos;
        this.acceso = new AccesoAEmpleados(empleados);
    }

    @Override
    public Permiso solicitar(
            Actor actor,
            UUID empleadoId,
            TipoPermiso tipo,
            LocalDate desde,
            LocalDate hasta,
            boolean conGoce,
            String motivo) {
        UUID paraQuien;
        if (actor.gestionaPersonal()) {
            if (empleadoId == null) {
                throw new ReglaDeNegocioException("Indique el empleado del permiso.");
            }
            paraQuien = acceso.buscar(empleadoId).id();
        } else {
            UUID yo = acceso.empleadoDe(actor).id();
            if (empleadoId != null && !empleadoId.equals(yo)) {
                throw new AccesoProhibidoException("Solo puedes solicitar permisos para ti.");
            }
            paraQuien = yo;
        }
        return permisos.guardar(Permiso.solicitar(paraQuien, tipo, desde, hasta, conGoce, motivo));
    }

    @Override
    public Permiso aprobar(Actor actor, UUID permisoId) {
        Permiso permiso = resolverPrevio(actor, permisoId);
        permiso.aprobar();
        Permiso guardado = permisos.guardar(permiso);
        eventos.publicar(new PermisoAprobado(
                guardado.id(),
                guardado.empleadoId(),
                TenantContext.requerido(),
                guardado.desde(),
                guardado.hasta(),
                guardado.conGoce()));
        return guardado;
    }

    @Override
    public Permiso rechazar(Actor actor, UUID permisoId, String motivo) {
        Permiso permiso = resolverPrevio(actor, permisoId);
        permiso.rechazar(motivo);
        return permisos.guardar(permiso);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Permiso> listar(Actor actor, EstadoPermiso estado) {
        if (actor.gestionaPersonal()) {
            return permisos.listar(null, estado);
        }
        return permisos.listar(List.of(acceso.empleadoDe(actor).id()), estado);
    }

    private Permiso resolverPrevio(Actor actor, UUID permisoId) {
        if (!actor.gestionaPersonal()) {
            throw new AccesoProhibidoException("Solo RRHH puede resolver permisos.");
        }
        Permiso permiso = permisos.buscar(permisoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el permiso solicitado."));
        acceso.exigirQueNoSeaElMismo(actor, permiso.empleadoId());
        return permiso;
    }
}
