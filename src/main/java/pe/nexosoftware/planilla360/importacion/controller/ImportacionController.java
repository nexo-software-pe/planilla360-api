package pe.nexosoftware.planilla360.importacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.nexosoftware.planilla360.importacion.dto.ResultadoImportacion;
import pe.nexosoftware.planilla360.importacion.service.ImportacionEmpleadosService;

@RestController
@RequestMapping("/api/v1/importaciones")
@PreAuthorize("hasAnyRole('RRHH', 'ADMIN_EMPRESA')")
@Tag(name = "Importaciones", description = "Carga masiva desde Excel, validada fila a fila")
public class ImportacionController {

    private static final String TIPO_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ImportacionEmpleadosService empleados;

    public ImportacionController(ImportacionEmpleadosService empleados) {
        this.empleados = empleados;
    }

    @PostMapping(value = "/empleados", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa empleados (con su primer contrato) desde un Excel",
            description = "Por defecto solo valida (`simular=true`) y devuelve todos los errores con fila y columna. "
                    + "Con `simular=false` guarda las filas válidas y omite las inválidas.")
    public ResultadoImportacion importarEmpleados(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(defaultValue = "true") boolean simular) {
        return empleados.importar(archivo, simular);
    }

    @GetMapping("/empleados/plantilla.xlsx")
    @Operation(summary = "Plantilla de Excel para importar empleados")
    public ResponseEntity<byte[]> plantilla() {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            var hoja = libro.createSheet("Empleados");
            Row encabezado = hoja.createRow(0);
            String[] columnas = ImportacionEmpleadosService.COLUMNAS;
            for (int i = 0; i < columnas.length; i++) {
                encabezado.createCell(i).setCellValue(columnas[i]);
                hoja.setColumnWidth(i, 20 * 256);
            }
            Row ejemplo = hoja.createRow(1);
            String[] valores = {"45678901", "Carla", "Mendoza Ríos", "1994-06-12", "cmendoza@empresa.pe", "987654321",
                "2026-11-03", "Producción", "Operario", "INDEFINIDO", "", "1500", "48", "AFP", "Integra", "FLUJO"};
            for (int i = 0; i < valores.length; i++) {
                ejemplo.createCell(i).setCellValue(valores[i]);
            }
            libro.write(salida);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(TIPO_XLSX))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename("plantilla-empleados.xlsx").build().toString())
                    .body(salida.toByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
