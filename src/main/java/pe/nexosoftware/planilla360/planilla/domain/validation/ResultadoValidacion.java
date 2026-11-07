package pe.nexosoftware.planilla360.planilla.domain.validation;

import java.util.List;

/** Resultado de revisar una planilla antes de cerrarla: la lista de incidencias (vacía si todo está en orden). */
public record ResultadoValidacion(List<Incidencia> incidencias) {

    public ResultadoValidacion {
        incidencias = List.copyOf(incidencias);
    }

    public boolean esValida() {
        return incidencias.isEmpty();
    }
}
