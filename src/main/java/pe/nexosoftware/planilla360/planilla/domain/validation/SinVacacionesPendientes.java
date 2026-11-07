package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** No debe haber vacaciones por aprobar que se solapen con el periodo. */
public final class SinVacacionesPendientes extends EslabonValidacion {

    @Override
    protected List<Incidencia> revisar(ContextoCierre contexto) {
        int pendientes = contexto.vacacionesPendientesQueSolapan();
        if (pendientes == 0) {
            return List.of();
        }
        return List.of(new Incidencia("VACACIONES_PENDIENTES",
                "Hay " + pendientes + " solicitudes de vacaciones por aprobar que se solapan con el periodo"));
    }
}
