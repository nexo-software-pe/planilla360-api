package pe.nexosoftware.planilla360.asistencia.domain.service;

import java.time.ZoneId;
import pe.nexosoftware.planilla360.asistencia.domain.model.AsistenciaDiaria;
import pe.nexosoftware.planilla360.asistencia.domain.model.ContextoDia;

/** Convierte las marcaciones de un día en tardanza, horas extra y estado. (Esqueleto del commit ROJO.) */
public final class ConsolidadorAsistencia {

    public static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final ZoneId zona;

    public ConsolidadorAsistencia() {
        this(ZONA_LIMA);
    }

    public ConsolidadorAsistencia(ZoneId zona) {
        this.zona = zona;
    }

    public AsistenciaDiaria consolidar(ContextoDia contexto) {
        throw new UnsupportedOperationException("pendiente");
    }
}
