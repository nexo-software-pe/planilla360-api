package pe.nexosoftware.planilla360.catalogo.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.catalogo.dto.AreaRequest;
import pe.nexosoftware.planilla360.catalogo.dto.AreaResponse;
import pe.nexosoftware.planilla360.catalogo.service.AreaService;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;

@RestController
@RequestMapping("/api/v1/areas-de-trabajo")
@Tag(name = "Áreas", description = "Áreas de la empresa (lectura: todos; escritura: RRHH / admin de empresa)")
public class AreaController {

    private static final String ESCRITURA = "hasAnyRole('RRHH', 'ADMIN_EMPRESA')";

    private final AreaService service;

    public AreaController(AreaService service) {
        this.service = service;
    }

    @GetMapping
    public Pagina<AreaResponse> listar(@PageableDefault(size = 50, sort = "nombre") Pageable pageable) {
        return service.listar(pageable);
    }

    @GetMapping("/{id}")
    public AreaResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<AreaResponse> crear(@Valid @RequestBody AreaRequest request) {
        AreaResponse creada = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/areas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ESCRITURA)
    public AreaResponse actualizar(@PathVariable UUID id, @Valid @RequestBody AreaRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ESCRITURA)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        service.eliminar(id);
    }
}
