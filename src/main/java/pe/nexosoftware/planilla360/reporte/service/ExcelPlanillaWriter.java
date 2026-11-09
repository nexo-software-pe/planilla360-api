package pe.nexosoftware.planilla360.reporte.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.reporte.dto.Reportes.FilaPlanilla;

/** Arma el libro de Excel (.xlsx) de una planilla con Apache POI. */
@Component
public class ExcelPlanillaWriter {

    private static final String[] ENCABEZADOS = {
        "DNI", "Trabajador", "Área", "Cargo", "Básico", "Asig. familiar", "Horas extra", "Gratificación", "CTS",
        "Total ingresos", "Total descuentos", "Neto", "EsSalud (empleador)"
    };

    public byte[] generar(String titulo, List<FilaPlanilla> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet(titulo);
            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle estiloEncabezado = libro.createCellStyle();
            estiloEncabezado.setFont(negrita);
            DataFormat formato = libro.createDataFormat();
            CellStyle estiloDinero = libro.createCellStyle();
            estiloDinero.setDataFormat(formato.getFormat("#,##0.00"));

            Row encabezado = hoja.createRow(0);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                encabezado.createCell(i).setCellValue(ENCABEZADOS[i]);
                encabezado.getCell(i).setCellStyle(estiloEncabezado);
            }
            int numero = 1;
            for (FilaPlanilla f : filas) {
                Row fila = hoja.createRow(numero++);
                fila.createCell(0).setCellValue(f.dni());
                fila.createCell(1).setCellValue(f.trabajador());
                fila.createCell(2).setCellValue(f.area());
                fila.createCell(3).setCellValue(f.cargo());
                BigDecimal[] montos = {
                    f.basico(), f.asignacionFamiliar(), f.horasExtra(), f.gratificacion(), f.cts(),
                    f.totalIngresos(), f.totalDescuentos(), f.neto(), f.essalud()
                };
                for (int i = 0; i < montos.length; i++) {
                    var celda = fila.createCell(4 + i);
                    celda.setCellValue(montos[i].doubleValue());
                    celda.setCellStyle(estiloDinero);
                }
            }
            hoja.createFreezePane(0, 1);
            for (int i = 0; i < ENCABEZADOS.length; i++) {
                hoja.autoSizeColumn(i);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo armar el Excel de la planilla", e);
        }
    }
}
