package pe.nexosoftware.planilla360.compartido.eventos;

/**
 * Marca que el hilo actual está cargando datos históricos (el seed local): los cierres de planilla de esa carga se
 * hacen con las mismas reglas, pero no deben disparar avisos (boletas masivas ni correos) de meses ya pasados.
 */
public final class CargaHistorica implements AutoCloseable {

    private static final ThreadLocal<Boolean> ACTIVA = ThreadLocal.withInitial(() -> false);

    private CargaHistorica() {
    }

    public static CargaHistorica iniciar() {
        ACTIVA.set(true);
        return new CargaHistorica();
    }

    public static boolean activa() {
        return ACTIVA.get();
    }

    @Override
    public void close() {
        ACTIVA.remove();
    }
}
