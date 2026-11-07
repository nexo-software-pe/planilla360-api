package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** La asistencia del mes debe estar consolidada antes de cerrar. */
public final class AsistenciaDelMesCompleta extends EslabonValidacion {

    @Override
    protected List<Incidencia> revisar(ContextoCierre contexto) {
        if (contexto.asistenciaCompleta()) {
            return List.of();
        }
        return List.of(new Incidencia("ASISTENCIA_INCOMPLETA",
                "La asistencia del periodo " + contexto.planilla().periodo() + " no está completa"));
    }
}
