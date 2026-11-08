package pe.nexosoftware.planilla360.empleado.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.empleado.dto.CeseRequest;
import pe.nexosoftware.planilla360.empleado.dto.ContratoRequest;
import pe.nexosoftware.planilla360.empleado.dto.ContratoResponse;
import pe.nexosoftware.planilla360.empleado.dto.EmpleadoRequest;
import pe.nexosoftware.planilla360.empleado.dto.EmpleadoResponse;
import pe.nexosoftware.planilla360.empleado.dto.FamiliarRequest;
import pe.nexosoftware.planilla360.empleado.dto.FamiliarResponse;
import pe.nexosoftware.planilla360.empleado.dto.FiltroEmpleados;
import pe.nexosoftware.planilla360.empleado.model.EstadoEmpleado;
import pe.nexosoftware.planilla360.empleado.service.ContratoService;
import pe.nexosoftware.planilla360.empleado.service.EmpleadoService;
import pe.nexosoftware.planilla360.empleado.service.FamiliarService;

@RestController
@RequestMapping("/api/v1/empleados")
@PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
@Tag(name = "Empleados", description = "Personal de la empresa, contratos y familiares (RRHH / admin de empresa)")
public class EmpleadoController {

    private final EmpleadoService empleados;
    private final ContratoService contratos;
    private final FamiliarService familiares;

    public EmpleadoController(EmpleadoService empleados, ContratoService contratos, FamiliarService familiares) {
        this.empleados = empleados;
        this.contratos = contratos;
        this.familiares = familiares;
    }

    @GetMapping
    public Pagina<EmpleadoResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID areaId,
            @RequestParam(required = false) UUID cargoId,
            @RequestParam(required = false) UUID jefeId,
            @RequestParam(required = false) EstadoEmpleado estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ingresoDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ingresoHasta,
            @PageableDefault(size = 20, sort = {"apellidos", "nombres"}) Pageable pageable) {
        return empleados.listar(
                new FiltroEmpleados(q, areaId, cargoId, jefeId, estado, ingresoDesde, ingresoHasta), pageable);
    }

    @PostMapping
    public ResponseEntity<EmpleadoResponse> crear(@Valid @RequestBody EmpleadoRequest request) {
        EmpleadoResponse creado = empleados.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/empleados/" + creado.id())).body(creado);
    }

    @GetMapping("/{id}")
    public EmpleadoResponse obtener(@PathVariable UUID id) {
        return empleados.obtener(id);
    }

    @PutMapping("/{id}")
    public EmpleadoResponse actualizar(@PathVariable UUID id, @Valid @RequestBody EmpleadoRequest request) {
        return empleados.actualizar(id, request);
    }

    @PatchMapping("/{id}/cese")
    public EmpleadoResponse cesar(@PathVariable UUID id, @Valid @RequestBody CeseRequest request) {
        return empleados.cesar(id, request);
    }

    @GetMapping("/{id}/contratos")
    public List<ContratoResponse> listarContratos(@PathVariable UUID id) {
        return contratos.listar(id);
    }

    @PostMapping("/{id}/contratos")
    public ResponseEntity<ContratoResponse> crearContrato(
            @PathVariable UUID id, @Valid @RequestBody ContratoRequest request) {
        ContratoResponse creado = contratos.crear(id, request);
        return ResponseEntity.created(URI.create("/api/v1/empleados/" + id + "/contratos/" + creado.id()))
                .body(creado);
    }

    @GetMapping("/{id}/familiares")
    public List<FamiliarResponse> listarFamiliares(@PathVariable UUID id) {
        return familiares.listar(id);
    }

    @PostMapping("/{id}/familiares")
    public ResponseEntity<FamiliarResponse> crearFamiliar(
            @PathVariable UUID id, @Valid @RequestBody FamiliarRequest request) {
        FamiliarResponse creado = familiares.crear(id, request);
        return ResponseEntity.created(URI.create("/api/v1/empleados/" + id + "/familiares/" + creado.id()))
                .body(creado);
    }
}
