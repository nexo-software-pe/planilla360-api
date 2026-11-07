package pe.nexosoftware.planilla360.catalogo.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.catalogo.dto.AfpResponse;
import pe.nexosoftware.planilla360.catalogo.dto.FeriadoResponse;
import pe.nexosoftware.planilla360.catalogo.dto.ParametroLegalRequest;
import pe.nexosoftware.planilla360.catalogo.dto.ParametroLegalResponse;
import pe.nexosoftware.planilla360.catalogo.service.CatalogoService;

/** Catálogos globales de solo lectura para todos los usuarios; los parámetros los mantiene la plataforma. */
@RestController
@RequestMapping("/api/v1")
@Validated
@Tag(name = "Catálogos globales", description = "AFP, parámetros legales y feriados")
public class CatalogoGlobalController {

    private final CatalogoService service;

    public CatalogoGlobalController(CatalogoService service) {
        this.service = service;
    }

    @GetMapping("/afps")
    public List<AfpResponse> afps() {
        return service.listarAfps();
    }

    @GetMapping("/parametros-legales")
    public List<ParametroLegalResponse> parametros(@RequestParam(required = false) String codigo) {
        return service.listarParametros(codigo);
    }

    @PostMapping("/parametros-legales")
    @PreAuthorize("hasRole('ADMIN_SAAS')")
    @ResponseStatus(HttpStatus.CREATED)
    public ParametroLegalResponse registrarVigencia(@Valid @RequestBody ParametroLegalRequest request) {
        return service.registrarVigencia(request);
    }

    @GetMapping("/feriados")
    public List<FeriadoResponse> feriados(
            @RequestParam(required = false) @Min(2000) @Max(2100) Integer anio) {
        return service.listarFeriados(anio == null ? LocalDate.now().getYear() : anio);
    }
}
