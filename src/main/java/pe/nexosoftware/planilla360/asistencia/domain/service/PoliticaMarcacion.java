package pe.nexosoftware.planilla360.asistencia.domain.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import pe.nexosoftware.planilla360.asistencia.domain.excepcion.ReglaDeAsistenciaException;
import pe.nexosoftware.planilla360.asistencia.domain.model.Marcacion;
import pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion;

/**
 * Decide qué tipo tiene la próxima marcación del día (alterna entrada y salida) y rechaza las
 * inválidas: demasiado seguidas (doble clic), anteriores a la última, o más de las permitidas por día.
 */
public final class PoliticaMarcacion {

    public static final int SEGUNDOS_MINIMOS_ENTRE_MARCACIONES = 60;
    public static final int MAXIMO_MARCACIONES_POR_DIA = 8;

    public TipoMarcacion siguienteTipo(List<Marcacion> delDia, Instant ahora) {
        if (delDia.size() >= MAXIMO_MARCACIONES_POR_DIA) {
            throw new ReglaDeAsistenciaException(
                    "Se alcanzó el máximo de " + MAXIMO_MARCACIONES_POR_DIA + " marcaciones por día.");
        }
        if (delDia.isEmpty()) {
            return TipoMarcacion.ENTRADA;
        }
        Marcacion ultima = delDia.stream().max(Comparator.comparing(Marcacion::instante)).orElseThrow();
        Duration transcurrido = Duration.between(ultima.instante(), ahora);
        if (transcurrido.isNegative()) {
            throw new ReglaDeAsistenciaException("La marcación es anterior a la última registrada.");
        }
        if (transcurrido.getSeconds() < SEGUNDOS_MINIMOS_ENTRE_MARCACIONES) {
            throw new ReglaDeAsistenciaException("Debe pasar al menos un minuto entre dos marcaciones.");
        }
        return ultima.tipo() == TipoMarcacion.ENTRADA ? TipoMarcacion.SALIDA : TipoMarcacion.ENTRADA;
    }
}
