package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** No debe haber vacaciones por aprobar que se solapen con el periodo. Esqueleto. */
public final class SinVacacionesPendientes extends EslabonValidacion {

    @Override
    protected List<Incidencia> revisar(ContextoCierre contexto) {
        throw new UnsupportedOperationException("pendiente");
    }
}
