package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** Ningún trabajador de la planilla puede estar sin régimen pensionario. Esqueleto. */
public final class TrabajadoresConRegimenPensionario extends EslabonValidacion {

    @Override
    protected List<Incidencia> revisar(ContextoCierre contexto) {
        throw new UnsupportedOperationException("pendiente");
    }
}
