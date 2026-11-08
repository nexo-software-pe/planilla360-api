package pe.nexosoftware.planilla360.empleado.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.catalogo.CatalogoApi;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.empleado.dto.ContratoRequest;
import pe.nexosoftware.planilla360.empleado.dto.ContratoResponse;
import pe.nexosoftware.planilla360.empleado.entity.Contrato;
import pe.nexosoftware.planilla360.empleado.entity.Empleado;
import pe.nexosoftware.planilla360.empleado.mapper.EmpleadoMapper;
import pe.nexosoftware.planilla360.empleado.model.TipoContrato;
import pe.nexosoftware.planilla360.empleado.model.TipoRegimenPensionario;
import pe.nexosoftware.planilla360.empleado.repository.ContratoRepository;
import pe.nexosoftware.planilla360.empleado.repository.EmpleadoRepository;

/** Contratos de un empleado: el historial conserva todos; el vigente es el último. */
@Service
@Transactional(readOnly = true)
public class ContratoService {

    private static final String ENTIDAD = "CONTRATO";

    private final ContratoRepository contratos;
    private final EmpleadoRepository empleados;
    private final CatalogoApi catalogo;
    private final EmpleadoMapper mapper;
    private final ApplicationEventPublisher eventos;

    public ContratoService(
            ContratoRepository contratos,
            EmpleadoRepository empleados,
            CatalogoApi catalogo,
            EmpleadoMapper mapper,
            ApplicationEventPublisher eventos) {
        this.contratos = contratos;
        this.empleados = empleados;
        this.catalogo = catalogo;
        this.mapper = mapper;
        this.eventos = eventos;
    }

    public List<ContratoResponse> listar(UUID empleadoId) {
        exigirEmpleado(empleadoId);
        return contratos.findByEmpleadoIdOrderByFechaInicioDesc(empleadoId).stream()
                .map(mapper::aResponse)
                .toList();
    }

    /**
     * Registra un contrato nuevo. Debe empezar después del inicio del contrato más reciente; si ese
     * seguía abierto, se cierra el día anterior.
     */
    @Transactional
    public ContratoResponse crear(UUID empleadoId, ContratoRequest request) {
        UUID tenant = TenantContext.requerido();
        Empleado empleado = exigirEmpleado(empleadoId);
        if (empleado.estaCesado()) {
            throw new ReglaDeNegocioException("No se pueden registrar contratos a un empleado cesado.");
        }
        validar(empleado, request);

        List<Contrato> previos = contratos.findByEmpleadoIdOrderByFechaInicioDesc(empleadoId);
        if (!previos.isEmpty()) {
            Contrato ultimo = previos.get(0);
            if (!request.fechaInicio().isAfter(ultimo.getFechaInicio())) {
                throw new ReglaDeNegocioException(
                        "El nuevo contrato debe empezar después del " + ultimo.getFechaInicio() + ".");
            }
            if (ultimo.sigueVigenteEn(request.fechaInicio())) {
                ultimo.cerrarEl(request.fechaInicio().minusDays(1));
            }
        }
        Contrato contrato = contratos.saveAndFlush(new Contrato(
                empleadoId,
                request.tipo(),
                request.fechaInicio(),
                request.fechaFin(),
                request.sueldoBasico(),
                request.jornadaSemanal(),
                request.regimenPensionario(),
                request.afpId(),
                request.tipoComision(),
                request.cuspp()));
        ContratoResponse respuesta = mapper.aResponse(contrato);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, contrato.getId(), tenant, Accion.CREAR, null, respuesta));
        return respuesta;
    }

    private void validar(Empleado empleado, ContratoRequest request) {
        if (request.fechaInicio().isBefore(empleado.getFechaIngreso())) {
            throw new ReglaDeNegocioException("El contrato no puede empezar antes del ingreso del empleado.");
        }
        if (request.fechaFin() != null && request.fechaFin().isBefore(request.fechaInicio())) {
            throw new ReglaDeNegocioException("La fecha de fin no puede ser anterior a la de inicio.");
        }
        if (request.tipo() == TipoContrato.PLAZO_FIJO && request.fechaFin() == null) {
            throw new ReglaDeNegocioException("Un contrato a plazo fijo requiere fecha de fin.");
        }
        if (request.regimenPensionario() == TipoRegimenPensionario.AFP) {
            if (request.afpId() == null || request.tipoComision() == null) {
                throw new ReglaDeNegocioException("El régimen AFP requiere la AFP y el tipo de comisión.");
            }
            if (!catalogo.existeAfp(request.afpId())) {
                throw new RecursoNoEncontradoException("No existe la AFP indicada.");
            }
        } else if (request.afpId() != null || request.tipoComision() != null) {
            throw new ReglaDeNegocioException("El régimen ONP no lleva AFP ni tipo de comisión.");
        }
        if (request.tipo() != TipoContrato.PART_TIME) {
            validarSueldoMinimo(request);
        }
    }

    /**
     * El sueldo no puede ser menor a la RMV vigente al inicio del contrato. Si el catálogo no tiene RMV
     * para esa fecha (contratos históricos anteriores a los datos cargados), no hay con qué comparar y
     * se acepta.
     */
    private void validarSueldoMinimo(ContratoRequest request) {
        BigDecimal rmv = catalogo.parametroVigente("RMV", request.fechaInicio()).orElse(null);
        if (rmv == null) {
            return;
        }
        if (request.sueldoBasico().compareTo(rmv) < 0) {
            throw new ReglaDeNegocioException("El sueldo básico no puede ser menor a la RMV vigente (S/ " + rmv + ").");
        }
    }

    private Empleado exigirEmpleado(UUID empleadoId) {
        return empleados.findById(empleadoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el empleado solicitado."));
    }
}
