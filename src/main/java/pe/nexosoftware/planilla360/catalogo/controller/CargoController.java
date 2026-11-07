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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.catalogo.dto.CargoRequest;
import pe.nexosoftware.planilla360.catalogo.dto.CargoResponse;
import pe.nexosoftware.planilla360.catalogo.service.CargoService;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;

@RestController
@RequestMapping("/api/v1/cargos")
@Tag(name = "Cargos", description = "Cargos por área (lectura: todos; escritura: RRHH / admin de empresa)")
public class CargoController {

    private static final String ESCRITURA = "hasAnyRole('RRHH', 'ADMIN_EMPRESA')";

    private final CargoService service;

    public CargoController(CargoService service) {
        this.service = service;
    }

    @GetMapping
    public Pagina<CargoResponse> listar(
            @RequestParam(required = false) UUID areaId, @PageableDefault(size = 50, sort = "nombre") Pageable pageable) {
        return service.listar(areaId, pageable);
    }

    @GetMapping("/{id}")
    public CargoResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PostMapping
    @PreAuthorize(ESCRITURA)
    public ResponseEntity<CargoResponse> crear(@Valid @RequestBody CargoRequest request) {
        CargoResponse creado = service.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/cargos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ESCRITURA)
    public CargoResponse actualizar(@PathVariable UUID id, @Valid @RequestBody CargoRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ESCRITURA)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        service.eliminar(id);
    }
}
