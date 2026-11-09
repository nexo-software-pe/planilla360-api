package pe.nexosoftware.planilla360.boleta.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.boleta.dto.BoletaResponse;
import pe.nexosoftware.planilla360.boleta.dto.EstadoGeneracionResponse;
import pe.nexosoftware.planilla360.boleta.service.BoletaService;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Boletas", description = "Boletas de pago en PDF: generación masiva asíncrona, estado y descarga")
public class BoletaController {

    private final BoletaService boletas;

    public BoletaController(BoletaService boletas) {
        this.boletas = boletas;
    }

    @PostMapping("/planillas/{planillaId}/boletas/generar")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
    @Operation(summary = "Genera (o reintenta) las boletas de una planilla cerrada",
            description = "Es asíncrono: responde 202 y el avance se consulta en /estado. "
                    + "Repetirlo solo procesa lo pendiente o fallido.")
    public ResponseEntity<EstadoGeneracionResponse> generar(@PathVariable UUID planillaId) {
        return ResponseEntity.accepted().body(boletas.generarMasivo(planillaId));
    }

    @GetMapping("/planillas/{planillaId}/boletas/estado")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
    @Operation(summary = "Avance de la generación de boletas de la planilla")
    public EstadoGeneracionResponse estado(@PathVariable UUID planillaId) {
        return boletas.estado(planillaId);
    }

    @GetMapping("/planillas/{planillaId}/boletas")
    @PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
    @Operation(summary = "Boletas de la planilla")
    public Pagina<BoletaResponse> deLaPlanilla(
            @PathVariable UUID planillaId, @PageableDefault(size = 50) Pageable pageable) {
        return boletas.deLaPlanilla(planillaId, pageable);
    }

    @GetMapping("/boletas/mias")
    @PreAuthorize("hasRole('EMPLEADO')")
    @Operation(summary = "Mis boletas")
    public Pagina<BoletaResponse> mias(@PageableDefault(size = 12) Pageable pageable) {
        return boletas.mias(pageable);
    }

    @GetMapping("/boletas/{id}/pdf")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'RRHH', 'ADMIN_EMPRESA')")
    @Operation(summary = "Descarga el PDF de una boleta (el trabajador solo accede a las suyas)")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        byte[] contenido = boletas.pdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("boleta-" + id + ".pdf").build().toString())
                .body(contenido);
    }
}
