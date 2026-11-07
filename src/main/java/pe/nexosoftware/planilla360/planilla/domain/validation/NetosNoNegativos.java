package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** Ningún trabajador puede quedar con neto a pagar negativo. Esqueleto. */
public final class NetosNoNegativos extends EslabonValidacion {

    @Override
    protected List<Incidencia> revisar(ContextoCierre contexto) {
        throw new UnsupportedOperationException("pendiente");
    }
}
