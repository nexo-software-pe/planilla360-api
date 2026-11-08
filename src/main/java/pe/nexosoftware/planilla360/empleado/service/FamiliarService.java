package pe.nexosoftware.planilla360.empleado.service;

import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.empleado.dto.FamiliarRequest;
import pe.nexosoftware.planilla360.empleado.dto.FamiliarResponse;
import pe.nexosoftware.planilla360.empleado.entity.Familiar;
import pe.nexosoftware.planilla360.empleado.mapper.EmpleadoMapper;
import pe.nexosoftware.planilla360.empleado.repository.EmpleadoRepository;
import pe.nexosoftware.planilla360.empleado.repository.FamiliarRepository;

/** Familiares del empleado (base de la asignación familiar). */
@Service
@Transactional(readOnly = true)
public class FamiliarService {

    private static final String ENTIDAD = "FAMILIAR";

    private final FamiliarRepository familiares;
    private final EmpleadoRepository empleados;
    private final EmpleadoMapper mapper;
    private final ApplicationEventPublisher eventos;

    public FamiliarService(
            FamiliarRepository familiares,
            EmpleadoRepository empleados,
            EmpleadoMapper mapper,
            ApplicationEventPublisher eventos) {
        this.familiares = familiares;
        this.empleados = empleados;
        this.mapper = mapper;
        this.eventos = eventos;
    }

    public List<FamiliarResponse> listar(UUID empleadoId) {
        exigirEmpleado(empleadoId);
        return familiares.findByEmpleadoIdOrderByFechaNacimientoAsc(empleadoId).stream()
                .map(mapper::aResponse)
                .toList();
    }

    @Transactional
    public FamiliarResponse crear(UUID empleadoId, FamiliarRequest request) {
        UUID tenant = TenantContext.requerido();
        exigirEmpleado(empleadoId);
        Familiar familiar = familiares.saveAndFlush(new Familiar(
                empleadoId, request.parentesco(), request.nombres(), request.fechaNacimiento(), request.estudia()));
        FamiliarResponse respuesta = mapper.aResponse(familiar);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, familiar.getId(), tenant, Accion.CREAR, null, respuesta));
        return respuesta;
    }

    private void exigirEmpleado(UUID empleadoId) {
        if (!empleados.existsById(empleadoId)) {
            throw new RecursoNoEncontradoException("No existe el empleado solicitado.");
        }
    }
}
