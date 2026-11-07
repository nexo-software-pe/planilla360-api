package pe.nexosoftware.planilla360.empresa.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import pe.nexosoftware.planilla360.empresa.dto.EmpresaRequest;
import pe.nexosoftware.planilla360.empresa.dto.EmpresaResponse;
import pe.nexosoftware.planilla360.empresa.dto.EstadoEmpresaRequest;
import pe.nexosoftware.planilla360.empresa.service.EmpresaService;

@RestController
@RequestMapping("/api/v1/empresas")
@PreAuthorize("hasRole('ADMIN_SAAS')")
@Tag(name = "Empresas", description = "Alta y administración de empresas cliente (solo administrador de la plataforma)")
public class EmpresaController {

    private final EmpresaService service;

    public EmpresaController(EmpresaService service) {
        this.service = service;
    }

    @GetMapping
    public Pagina<EmpresaResponse> listar(
            @RequestParam(required = false) Boolean activa,
            @PageableDefault(size = 20, sort = "razonSocial") Pageable pageable) {
        return service.listar(activa, pageable);
    }

    @GetMapping("/{id}")
    public EmpresaResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EmpresaResponse> crear(@Valid @RequestBody EmpresaRequest request) {
        EmpresaResponse creada = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/empresas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public EmpresaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody EmpresaRequest request) {
        return service.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public EmpresaResponse cambiarEstado(@PathVariable UUID id, @Valid @RequestBody EstadoEmpresaRequest request) {
        return service.cambiarEstado(id, request.activa());
    }
}
