package pe.nexosoftware.planilla360.empleado.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.catalogo.CatalogoApi;
import pe.nexosoftware.planilla360.compartido.dominio.Dni;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.empleado.ContratoResumen;
import pe.nexosoftware.planilla360.empleado.EmpleadoApi;
import pe.nexosoftware.planilla360.empleado.EmpleadoResumen;
import pe.nexosoftware.planilla360.empleado.FichaEmpleado;
import pe.nexosoftware.planilla360.empleado.NuevoEmpleado;
import pe.nexosoftware.planilla360.empleado.dto.ContratoRequest;
import pe.nexosoftware.planilla360.empleado.model.TipoComision;
import pe.nexosoftware.planilla360.empleado.model.TipoContrato;
import pe.nexosoftware.planilla360.empleado.model.TipoRegimenPensionario;
import pe.nexosoftware.planilla360.empleado.HijoResumen;
import pe.nexosoftware.planilla360.empleado.dto.CeseRequest;
import pe.nexosoftware.planilla360.empleado.dto.EmpleadoRequest;
import pe.nexosoftware.planilla360.empleado.dto.EmpleadoResponse;
import pe.nexosoftware.planilla360.empleado.dto.FiltroEmpleados;
import pe.nexosoftware.planilla360.empleado.entity.Empleado;
import pe.nexosoftware.planilla360.empleado.entity.Contrato;
import pe.nexosoftware.planilla360.empleado.mapper.EmpleadoMapper;
import pe.nexosoftware.planilla360.empleado.model.Parentesco;
import pe.nexosoftware.planilla360.empleado.repository.ContratoRepository;
import pe.nexosoftware.planilla360.empleado.repository.EmpleadoRepository;
import pe.nexosoftware.planilla360.empleado.repository.FamiliarRepository;
import pe.nexosoftware.planilla360.identidad.IdentidadApi;

@Service
@Transactional(readOnly = true)
public class EmpleadoService implements EmpleadoApi {

    static final String ENTIDAD = "EMPLEADO";
    /** Edad mínima al ingresar (interpretación de negocio: personal adulto). */
    static final int EDAD_MINIMA = 18;
    private static final int PROFUNDIDAD_MAXIMA_JERARQUIA = 50;

    private final EmpleadoRepository empleados;
    private final ContratoRepository contratos;
    private final FamiliarRepository familiares;
    private final EmpleadoMapper mapper;
    private final CatalogoApi catalogo;
    private final IdentidadApi identidad;
    private final ApplicationEventPublisher eventos;
    private final ContratoService contratoService;

    public EmpleadoService(
            EmpleadoRepository empleados,
            ContratoRepository contratos,
            FamiliarRepository familiares,
            EmpleadoMapper mapper,
            CatalogoApi catalogo,
            IdentidadApi identidad,
            ApplicationEventPublisher eventos,
            ContratoService contratoService) {
        this.empleados = empleados;
        this.contratos = contratos;
        this.familiares = familiares;
        this.mapper = mapper;
        this.catalogo = catalogo;
        this.identidad = identidad;
        this.eventos = eventos;
        this.contratoService = contratoService;
    }

    public Pagina<EmpleadoResponse> listar(FiltroEmpleados filtro, Pageable pageable) {
        Page<Empleado> page = empleados.findAll(EmpleadoSpecs.desde(filtro), pageable);
        return Pagina.de(page, mapper::aResponse);
    }

    public EmpleadoResponse obtener(UUID id) {
        return mapper.aResponse(buscarEntidad(id));
    }

    @Transactional
    public EmpleadoResponse crear(EmpleadoRequest request) {
        UUID tenant = TenantContext.requerido();
        if (!Dni.esValido(request.dni())) {
            throw new ReglaDeNegocioException("El DNI debe tener 8 dígitos.");
        }
        if (empleados.existsByDni(request.dni())) {
            throw new ConflictoException("Ya existe un empleado con el DNI " + request.dni() + ".");
        }
        validarDatos(request, null);
        Empleado empleado = empleados.saveAndFlush(new Empleado(
                new Dni(request.dni()),
                request.nombres(),
                request.apellidos(),
                request.fechaNacimiento(),
                request.email(),
                request.telefono(),
                request.fechaIngreso(),
                request.areaId(),
                request.cargoId(),
                request.jefeId()));
        EmpleadoResponse respuesta = mapper.aResponse(empleado);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, empleado.getId(), tenant, Accion.CREAR, null, respuesta));
        return respuesta;
    }

    @Transactional
    public EmpleadoResponse actualizar(UUID id, EmpleadoRequest request) {
        UUID tenant = TenantContext.requerido();
        Empleado empleado = buscarEntidad(id);
        if (!empleado.getDni().equals(request.dni())) {
            throw new ConflictoException("El DNI de un empleado no puede modificarse.");
        }
        if (empleado.estaCesado()) {
            throw new ReglaDeNegocioException("No se puede modificar a un empleado cesado.");
        }
        validarDatos(request, id);
        EmpleadoResponse antes = mapper.aResponse(empleado);
        empleado.actualizar(
                request.nombres(),
                request.apellidos(),
                request.fechaNacimiento(),
                request.email(),
                request.telefono(),
                request.fechaIngreso(),
                request.areaId(),
                request.cargoId(),
                request.jefeId());
        empleados.flush();
        EmpleadoResponse despues = mapper.aResponse(empleado);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, id, tenant, Accion.ACTUALIZAR, antes, despues));
        return despues;
    }

    @Transactional
    public EmpleadoResponse cesar(UUID id, CeseRequest request) {
        UUID tenant = TenantContext.requerido();
        Empleado empleado = buscarEntidad(id);
        if (empleado.estaCesado()) {
            throw new ConflictoException("El empleado ya fue cesado.");
        }
        if (request.fechaCese().isBefore(empleado.getFechaIngreso())) {
            throw new ReglaDeNegocioException("La fecha de cese no puede ser anterior a la fecha de ingreso.");
        }
        EmpleadoResponse antes = mapper.aResponse(empleado);
        empleado.cesar(request.fechaCese());
        empleados.flush();
        EmpleadoResponse despues = mapper.aResponse(empleado);
        eventos.publishEvent(new CambioRegistrado(ENTIDAD, id, tenant, Accion.ACTUALIZAR, antes, despues));
        return despues;
    }

    @Override
    public Optional<EmpleadoResumen> buscar(UUID id) {
        return empleados.findById(id).map(mapper::aResumen);
    }

    @Override
    public Optional<EmpleadoResumen> deUsuario(UUID usuarioId) {
        return identidad.empleadoIdDe(usuarioId).flatMap(this::buscar);
    }

    @Override
    public List<EmpleadoResumen> vigentesEn(LocalDate fecha) {
        return empleados.findVigentesEn(fecha).stream().map(mapper::aResumen).toList();
    }

    @Override
    public List<EmpleadoResumen> vigentesEntre(LocalDate desde, LocalDate hasta) {
        return empleados.findVigentesEntre(desde, hasta).stream().map(mapper::aResumen).toList();
    }

    @Override
    public Optional<ContratoResumen> contratoVigenteEn(UUID empleadoId, LocalDate fecha) {
        return contratos.findByEmpleadoIdOrderByFechaInicioDesc(empleadoId).stream()
                .filter(c -> !c.getFechaInicio().isAfter(fecha) && c.sigueVigenteEn(fecha))
                .findFirst()
                .map(EmpleadoService::aResumen);
    }

    @Override
    public List<HijoResumen> hijosDe(UUID empleadoId) {
        return familiares.findByEmpleadoIdOrderByFechaNacimientoAsc(empleadoId).stream()
                .filter(f -> f.getParentesco() == Parentesco.HIJO)
                .map(f -> new HijoResumen(f.getFechaNacimiento(), f.isEstudia()))
                .toList();
    }

    @Override
    public Optional<FichaEmpleado> fichaDe(UUID empleadoId) {
        return empleados.findById(empleadoId).map(e -> new FichaEmpleado(
                e.getId(),
                e.getDni(),
                e.nombreCompleto(),
                e.getEmail(),
                catalogo.nombreDeArea(e.getAreaId()).orElse(""),
                catalogo.nombreDeCargo(e.getCargoId()).orElse(""),
                e.getFechaIngreso()));
    }

    @Override
    public boolean existeDni(String dni) {
        return empleados.existsByDni(dni);
    }

    @Override
    @Transactional
    public UUID registrar(NuevoEmpleado datos) {
        EmpleadoResponse empleado = crear(new EmpleadoRequest(
                datos.dni(),
                datos.nombres(),
                datos.apellidos(),
                datos.fechaNacimiento(),
                datos.email(),
                datos.telefono(),
                datos.fechaIngreso(),
                datos.areaId(),
                datos.cargoId(),
                null));
        contratoService.crear(empleado.id(), new ContratoRequest(
                TipoContrato.valueOf(datos.tipoContrato()),
                datos.fechaIngreso(),
                datos.fechaFinContrato(),
                datos.sueldoBasico(),
                datos.jornadaSemanal(),
                null,
                TipoRegimenPensionario.valueOf(datos.regimenPensionario()),
                datos.afpId(),
                datos.tipoComision() == null ? null : TipoComision.valueOf(datos.tipoComision()),
                null));
        return empleado.id();
    }

    private static ContratoResumen aResumen(Contrato contrato) {
        return new ContratoResumen(
                contrato.getEmpleadoId(),
                contrato.getSueldoBasico(),
                contrato.getJornadaSemanal(),
                contrato.getDiasLaborables(),
                contrato.getRegimenPensionario().name(),
                contrato.getAfpId(),
                contrato.getTipoComision() == null ? null : contrato.getTipoComision().name());
    }

    private void validarDatos(EmpleadoRequest request, UUID idPropio) {
        if (!catalogo.existeArea(request.areaId())) {
            throw new RecursoNoEncontradoException("No existe el área indicada.");
        }
        if (!catalogo.existeCargo(request.cargoId())) {
            throw new RecursoNoEncontradoException("No existe el cargo indicado.");
        }
        if (request.fechaNacimiento().plusYears(EDAD_MINIMA).isAfter(request.fechaIngreso())) {
            throw new ReglaDeNegocioException("El empleado debe ser mayor de " + EDAD_MINIMA + " años al ingresar.");
        }
        if (request.jefeId() != null) {
            validarJefe(request.jefeId(), idPropio);
        }
    }

    /** El jefe debe existir en la empresa, estar vigente y no crear un ciclo en la jerarquía. */
    private void validarJefe(UUID jefeId, UUID idPropio) {
        if (jefeId.equals(idPropio)) {
            throw new ReglaDeNegocioException("Un empleado no puede ser su propio jefe.");
        }
        Empleado jefe = empleados.findById(jefeId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el jefe indicado."));
        if (jefe.estaCesado()) {
            throw new ReglaDeNegocioException("El jefe indicado está cesado.");
        }
        UUID actual = jefe.getJefeId();
        for (int nivel = 0; actual != null && nivel < PROFUNDIDAD_MAXIMA_JERARQUIA; nivel++) {
            if (actual.equals(idPropio)) {
                throw new ReglaDeNegocioException("La jerarquía de jefes no puede tener ciclos.");
            }
            actual = empleados.findById(actual).map(Empleado::getJefeId).orElse(null);
        }
    }

    private Empleado buscarEntidad(UUID id) {
        return empleados.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el empleado solicitado."));
    }
}
