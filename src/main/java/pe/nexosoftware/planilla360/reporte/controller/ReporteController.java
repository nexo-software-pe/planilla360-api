package pe.nexosoftware.planilla360.reporte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.reporte.dto.Reportes.CostoLaboral;
import pe.nexosoftware.planilla360.reporte.dto.Reportes.TardanzaPorArea;
import pe.nexosoftware.planilla360.reporte.dto.Reportes.VacacionPendiente;
import pe.nexosoftware.planilla360.reporte.service.ExcelPlanillaWriter;
import pe.nexosoftware.planilla360.reporte.service.ReporteService;

@RestController
@RequestMapping("/api/v1/reportes")
@PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
@Tag(name = "Reportes", description = "Reportes de solo lectura con SQL nativo y exportación a Excel")
public class ReporteController {

    private static final String TIPO_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ReporteService reportes;
    private final ExcelPlanillaWriter excel;

    public ReporteController(ReporteService reportes, ExcelPlanillaWriter excel) {
        this.reportes = reportes;
        this.excel = excel;
    }

    @GetMapping("/tardanzas-por-area")
    @Operation(summary = "Los trabajadores con más tardanzas de cada área en el mes",
            description = "Usa una CTE y RANK() por área. `top` limita cuántos por área (por defecto 3; los empates comparten lugar).")
    public List<TardanzaPorArea> tardanzasPorArea(
            @RequestParam String periodo, @RequestParam(defaultValue = "3") int top) {
        if (top < 1 || top > 50) {
            throw new ReglaDeNegocioException("`top` debe estar entre 1 y 50.");
        }
        return reportes.tardanzasPorArea(periodo(periodo), top);
    }

    @GetMapping("/costo-laboral")
    @Operation(summary = "Costo laboral (ingresos + EsSalud) por área y cargo, con subtotales y total",
            description = "Usa ROLLUP sobre la planilla mensual cerrada del periodo.")
    public List<CostoLaboral> costoLaboral(@RequestParam String periodo) {
        return reportes.costoLaboral(periodo(periodo));
    }

    @GetMapping("/vacaciones-pendientes")
    @Operation(summary = "Saldos de vacaciones por trabajador y periodo, con el saldo total acumulado")
    public List<VacacionPendiente> vacacionesPendientes() {
        return reportes.vacacionesPendientes();
    }

    @GetMapping("/planilla-mensual.xlsx")
    @Operation(summary = "Excel con el detalle de la planilla cerrada del periodo")
    public ResponseEntity<byte[]> planillaMensual(
            @RequestParam String periodo, @RequestParam(defaultValue = "MENSUAL") String tipo) {
        YearMonth mes = periodo(periodo);
        if (!List.of("MENSUAL", "GRATIFICACION", "CTS").contains(tipo)) {
            throw new ReglaDeNegocioException("El tipo debe ser MENSUAL, GRATIFICACION o CTS.");
        }
        byte[] libro = excel.generar("Planilla " + mes, reportes.planilla(mes, tipo));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(TIPO_XLSX))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("planilla-" + tipo.toLowerCase() + "-" + mes + ".xlsx").build().toString())
                .body(libro);
    }

    private static YearMonth periodo(String texto) {
        try {
            return YearMonth.parse(texto);
        } catch (DateTimeParseException e) {
            throw new ReglaDeNegocioException("El periodo debe tener el formato AAAA-MM (por ejemplo 2026-10).");
        }
    }
}
