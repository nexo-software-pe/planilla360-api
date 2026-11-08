package pe.nexosoftware.planilla360.asistencia.domain.service;

import java.time.Instant;
import java.util.List;
import pe.nexosoftware.planilla360.asistencia.domain.model.Marcacion;
import pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion;

/** Decide qué tipo tiene la próxima marcación y rechaza las inválidas. (Esqueleto del commit ROJO.) */
public final class PoliticaMarcacion {

    public static final int SEGUNDOS_MINIMOS_ENTRE_MARCACIONES = 60;
    public static final int MAXIMO_MARCACIONES_POR_DIA = 8;

    public TipoMarcacion siguienteTipo(List<Marcacion> delDia, Instant ahora) {
        throw new UnsupportedOperationException("pendiente");
    }
}
