package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** Resultado de revisar una planilla antes de cerrarla. Esqueleto. */
public record ResultadoValidacion(List<Incidencia> incidencias) {

    public boolean esValida() {
        throw new UnsupportedOperationException("pendiente");
    }
}
