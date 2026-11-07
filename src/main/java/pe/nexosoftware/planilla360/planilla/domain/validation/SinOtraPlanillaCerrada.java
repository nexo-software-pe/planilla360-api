package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** No puede haber otra planilla cerrada del mismo periodo y tipo. Esqueleto. */
public final class SinOtraPlanillaCerrada extends EslabonValidacion {

    @Override
    protected List<Incidencia> revisar(ContextoCierre contexto) {
        throw new UnsupportedOperationException("pendiente");
    }
}
