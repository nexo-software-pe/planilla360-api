package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** No puede haber otra planilla cerrada del mismo periodo y tipo. */
public final class SinOtraPlanillaCerrada extends EslabonValidacion {

    @Override
    protected List<Incidencia> revisar(ContextoCierre contexto) {
        if (!contexto.existeOtraPlanillaCerradaDelPeriodo()) {
            return List.of();
        }
        return List.of(new Incidencia("PLANILLA_DUPLICADA",
                "Ya existe una planilla " + contexto.planilla().tipo() + " cerrada del periodo "
                        + contexto.planilla().periodo()));
    }
}
