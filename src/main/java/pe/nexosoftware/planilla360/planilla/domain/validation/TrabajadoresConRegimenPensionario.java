package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** Ningún trabajador de la planilla puede estar sin régimen pensionario. */
public final class TrabajadoresConRegimenPensionario extends EslabonValidacion {

    @Override
    protected List<Incidencia> revisar(ContextoCierre contexto) {
        return contexto.empleadosSinRegimenPensionario().stream()
                .map(id -> new Incidencia("SIN_REGIMEN_PENSIONARIO",
                        "El trabajador " + id + " no tiene régimen pensionario"))
                .toList();
    }
}
