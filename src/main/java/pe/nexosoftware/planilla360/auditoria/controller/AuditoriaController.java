package pe.nexosoftware.planilla360.auditoria.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.auditoria.dto.AuditoriaResponse;
import pe.nexosoftware.planilla360.auditoria.service.AuditoriaService;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;

@RestController
@RequestMapping("/api/v1/auditoria")
@PreAuthorize("hasAnyRole('ADMIN_SAAS', 'ADMIN_EMPRESA')")
@Tag(name = "Auditoría", description = "Historial de cambios de entidades sensibles")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public Pagina<AuditoriaResponse> listar(
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID tenantId,
            @PageableDefault(size = 20, sort = "instante", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.listar(entidad, desde, hasta, tenantId, pageable);
    }
}
