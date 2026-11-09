package pe.nexosoftware.planilla360.importacion.lector;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Adapter de {@link LectorHojaExcel} sobre Apache POI. Las fechas salen en formato ISO (AAAA-MM-DD). */
@Component
class PoiLectorHojaExcel implements LectorHojaExcel {

    private final DataFormatter formateador = new DataFormatter(Locale.ROOT);

    @Override
    public List<Fila> leer(InputStream contenido, int maximoFilas) {
        try (Workbook libro = new XSSFWorkbook(contenido)) {
            Sheet hoja = libro.getSheetAt(0);
            Row encabezado = hoja.getRow(hoja.getFirstRowNum());
            if (encabezado == null) {
                throw new IllegalArgumentException("La hoja no tiene encabezados.");
            }
            Map<Integer, String> columnas = new LinkedHashMap<>();
            for (Cell celda : encabezado) {
                String nombre = formateador.formatCellValue(celda).trim().toLowerCase(Locale.ROOT);
                if (!nombre.isEmpty()) {
                    columnas.put(celda.getColumnIndex(), nombre);
                }
            }
            List<Fila> filas = new ArrayList<>();
            for (int i = hoja.getFirstRowNum() + 1; i <= hoja.getLastRowNum(); i++) {
                Row fila = hoja.getRow(i);
                if (fila == null) {
                    continue;
                }
                Map<String, String> valores = new LinkedHashMap<>();
                columnas.forEach((indice, nombre) -> {
                    String texto = textoDe(fila.getCell(indice));
                    if (!texto.isEmpty()) {
                        valores.put(nombre, texto);
                    }
                });
                if (valores.isEmpty()) {
                    continue;
                }
                if (filas.size() >= maximoFilas) {
                    throw new IllegalArgumentException("El archivo supera el máximo de " + maximoFilas + " filas.");
                }
                filas.add(new Fila(i + 1, valores));
            }
            return filas;
        } catch (IOException | RuntimeException e) {
            if (e instanceof IllegalArgumentException ia && ia.getMessage() != null
                    && (ia.getMessage().startsWith("La hoja") || ia.getMessage().startsWith("El archivo supera"))) {
                throw ia;
            }
            throw new IllegalArgumentException("El archivo no es un libro de Excel (.xlsx) válido.", e);
        }
    }

    private String textoDe(Cell celda) {
        if (celda == null) {
            return "";
        }
        if (celda.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celda)) {
            LocalDate fecha = celda.getDateCellValue().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
            return fecha.toString();
        }
        return formateador.formatCellValue(celda).trim();
    }
}
