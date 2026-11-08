package pe.nexosoftware.planilla360.planilla.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;
import pe.nexosoftware.planilla360.planilla.application.model.Actor;
import pe.nexosoftware.planilla360.planilla.application.model.FiltroPlanillas;
import pe.nexosoftware.planilla360.planilla.application.model.ResultadoCierre;
import pe.nexosoftware.planilla360.planilla.application.model.ResumenPlanilla;
import pe.nexosoftware.planilla360.planilla.application.port.in.AnularPlanillaUseCase;
import pe.nexosoftware.planilla360.planilla.application.port.in.CerrarPlanillaUseCase;
import pe.nexosoftware.planilla360.planilla.application.port.in.GestionarPlanillasUseCase;
import pe.nexosoftware.planilla360.planilla.application.port.in.RegistrarPagoUseCase;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.AnularPlanillaRequest;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.CrearPlanillaRequest;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.GenerarBeneficioRequest;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.LineaPlanillaResponse;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.PagoPlanillaRequest;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.PagoPlanillaResponse;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.PlanillaResponse;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.dto.ValidacionResponse;
import pe.nexosoftware.planilla360.planilla.infrastructure.web.mapper.PlanillaWebMapper;

@RestController
@RequestMapping("/api/v1/planillas")
@PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
@Tag(name = "Planillas", description = "Planilla mensual, gratificación y CTS: cálculo, validación, cierre y anulación")
public class PlanillaController {

    static final String CABECERA_REPETIDO = "Idempotent-Replayed";

    private final GestionarPlanillasUseCase planillas;
    private final CerrarPlanillaUseCase cierre;
    private final AnularPlanillaUseCase anulacion;
    private final RegistrarPagoUseCase pagos;
    private final PlanillaWebMapper mapper;

    public PlanillaController(
            GestionarPlanillasUseCase planillas,
            CerrarPlanillaUseCase cierre,
            AnularPlanillaUseCase anulacion,
            RegistrarPagoUseCase pagos,
            PlanillaWebMapper mapper) {
        this.planillas = planillas;
        this.cierre = cierre;
        this.anulacion = anulacion;
        this.pagos = pagos;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Crea una planilla en borrador (mensual por defecto)")
    public ResponseEntity<PlanillaResponse> crear(@Valid @RequestBody CrearPlanillaRequest request) {
        TipoPlanilla tipo = request.tipo() == null ? TipoPlanilla.MENSUAL : request.tipo();
        PlanillaResponse creada = mapper.aResponse(planillas.crear(Periodo.parse(request.periodo()), tipo));
        return ResponseEntity.created(URI.create("/api/v1/planillas/" + creada.id())).body(creada);
    }

    @GetMapping
    @Operation(summary = "Lista las planillas, de la más reciente a la más antigua")
    public Pagina<PlanillaResponse> listar(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) TipoPlanilla tipo,
            @RequestParam(required = false) EstadoPlanilla estado,
            @PageableDefault(size = 20) Pageable pageable) {
        FiltroPlanillas filtro = new FiltroPlanillas(periodo == null ? null : Periodo.parse(periodo), tipo, estado);
        Pagina<ResumenPlanilla> pagina = planillas.listar(filtro, pageable.getPageNumber(), pageable.getPageSize());
        return new Pagina<>(
                pagina.contenido().stream().map(mapper::aResponse).toList(),
                pagina.pagina(),
                pagina.tamano(),
                pagina.totalElementos(),
                pagina.totalPaginas());
    }

    @GetMapping("/{id}")
    public PlanillaResponse obtener(@PathVariable UUID id) {
        return mapper.aResponse(planillas.obtener(id));
    }

    @PostMapping("/{id}/calcular")
    @Operation(summary = "Calcula (o recalcula) las líneas de una planilla en borrador o calculada")
    public PlanillaResponse calcular(@PathVariable UUID id) {
        return mapper.aResponse(planillas.calcular(id));
    }

    @GetMapping("/{id}/lineas")
    @Operation(summary = "Boletas de la planilla: una línea por trabajador con su desglose")
    public List<LineaPlanillaResponse> lineas(@PathVariable UUID id) {
        return planillas.obtener(id).lineas().stream().map(mapper::aResponse).toList();
    }

    @GetMapping("/{id}/validaciones")
    @Operation(summary = "Incidencias que impiden cerrar la planilla")
    public ValidacionResponse validaciones(@PathVariable UUID id) {
        return mapper.aResponse(planillas.validar(id));
    }

    @PostMapping("/{id}/cerrar")
    @Operation(
            summary = "Cierra la planilla de forma idempotente",
            description = "Repetir la petición con la misma Idempotency-Key devuelve la misma planilla cerrada "
                    + "(cabecera Idempotent-Replayed: true) sin cerrar de nuevo.")
    public ResponseEntity<PlanillaResponse> cerrar(
            @PathVariable UUID id, @RequestHeader("Idempotency-Key") String claveIdempotencia) {
        ResultadoCierre resultado = cierre.cerrar(id, claveIdempotencia);
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok();
        if (resultado.repetido()) {
            respuesta.header(CABECERA_REPETIDO, "true");
        }
        return respuesta.body(mapper.aResponse(resultado.planilla()));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    @Operation(summary = "Anula la planilla (solo el administrador de la empresa y sin pago registrado)")
    public PlanillaResponse anular(@PathVariable UUID id, @Valid @RequestBody AnularPlanillaRequest request) {
        UsuarioAutenticado usuario = UsuarioAutenticado.requerido();
        Planilla anulada = anulacion.anular(new Actor(usuario.usuarioId(), usuario.rol()), id, request.motivo());
        return mapper.aResponse(anulada);
    }

    @PostMapping("/{id}/pago")
    @Operation(summary = "Registra el pago de una planilla cerrada (desde entonces ya no se puede anular)")
    public ResponseEntity<PagoPlanillaResponse> registrarPago(
            @PathVariable UUID id, @Valid @RequestBody PagoPlanillaRequest request) {
        PagoPlanillaResponse pago = mapper.aResponse(pagos.registrar(id, request.fechaPago(), request.referencia()));
        return ResponseEntity.created(URI.create("/api/v1/planillas/" + id)).body(pago);
    }

    @PostMapping("/gratificacion")
    @Operation(
            summary = "Simula o genera la planilla de gratificación (julio y diciembre)",
            description = "Con simular=true (por defecto) solo devuelve el cálculo; con simular=false crea la "
                    + "planilla calculada, lista para validar y cerrar.")
    public ResponseEntity<?> gratificacion(@Valid @RequestBody GenerarBeneficioRequest request) {
        return beneficio(request, TipoPlanilla.GRATIFICACION);
    }

    @PostMapping("/cts")
    @Operation(
            summary = "Simula o genera la planilla de CTS (mayo y noviembre)",
            description = "Con simular=true (por defecto) solo devuelve el cálculo; con simular=false crea la "
                    + "planilla calculada, lista para validar y cerrar.")
    public ResponseEntity<?> cts(@Valid @RequestBody GenerarBeneficioRequest request) {
        return beneficio(request, TipoPlanilla.CTS);
    }

    private ResponseEntity<?> beneficio(GenerarBeneficioRequest request, TipoPlanilla tipo) {
        Periodo periodo = Periodo.parse(request.periodo());
        if (request.soloSimular()) {
            return ResponseEntity.ok(mapper.aSimulacion(planillas.simular(periodo, tipo)));
        }
        PlanillaResponse generada = mapper.aResponse(planillas.generar(periodo, tipo));
        return ResponseEntity.created(URI.create("/api/v1/planillas/" + generada.id())).body(generada);
    }
}
