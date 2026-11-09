package pe.nexosoftware.planilla360.importacion.lector;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/** Puerto de salida: lee la primera hoja de un libro de Excel como filas con columnas por nombre (patrón Adapter). */
public interface LectorHojaExcel {

    /**
     * @param numero número de fila tal como la ve el usuario en Excel (la primera de datos es la 2)
     * @param valores columnas por nombre de encabezado en minúsculas; las celdas vacías se omiten
     */
    record Fila(int numero, Map<String, String> valores) {
    }

    /** @throws IllegalArgumentException si el archivo no es un .xlsx válido o excede {@code maximoFilas} */
    List<Fila> leer(InputStream contenido, int maximoFilas);
}
