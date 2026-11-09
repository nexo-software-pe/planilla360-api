package pe.nexosoftware.planilla360.importacion.dto;

import java.util.List;

/**
 * Resultado de una importación.
 *
 * @param simulacion {@code true} si solo se validó y no se guardó nada
 * @param validas filas sin errores (en una simulación, las que se importarían)
 * @param importadas filas guardadas (siempre 0 en una simulación)
 */
public record ResultadoImportacion(
        boolean simulacion, int totalFilas, int validas, int importadas, List<ErrorFila> errores) {

    /** @param fila número de fila en Excel; {@code campo} puede ser nulo si el error es de la fila completa */
    public record ErrorFila(int fila, String campo, String mensaje) {
    }
}
