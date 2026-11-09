package pe.nexosoftware.planilla360.catalogo.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.catalogo.dto.AreaResponse;
import pe.nexosoftware.planilla360.catalogo.service.AreaService;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;

@RestController
@RequestMapping("/api/v2/areas")
@Tag(name = "Áreas v2", description = "Prueba de contrato: endpoint nuevo en v2")
public class AreaV2Controller {

    private final AreaService service;

    public AreaV2Controller(AreaService service) {
        this.service = service;
    }

    @GetMapping
    public Pagina<AreaResponse> listar(@PageableDefault(size = 50, sort = "nombre") Pageable pageable) {
        return service.listar(pageable);
    }
}
