package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** Eslabón de la cadena de validaciones de cierre. Esqueleto. */
public abstract class EslabonValidacion {

    public final EslabonValidacion luego(EslabonValidacion siguiente) {
        throw new UnsupportedOperationException("pendiente");
    }

    public final List<Incidencia> validar(ContextoCierre contexto) {
        throw new UnsupportedOperationException("pendiente");
    }

    /** Incidencias que detecta este eslabón (sin consultar a los demás). */
    protected abstract List<Incidencia> revisar(ContextoCierre contexto);
}
