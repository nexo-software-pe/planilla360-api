package pe.nexosoftware.planilla360.vacaciones.domain.service;

import java.time.LocalDate;
import java.util.List;
import pe.nexosoftware.planilla360.vacaciones.domain.model.PoliticaVacaciones;

/** Determina los periodos vacacionales ganados por un empleado. (Esqueleto del commit ROJO.) */
public final class CalculadoraPeriodos {

    /** Un año de servicio completo y los días que otorgó. */
    public record PeriodoCalculado(LocalDate desde, LocalDate hasta, int diasGanados) {
    }

    public List<PeriodoCalculado> periodosCumplidos(
            LocalDate fechaIngreso, LocalDate fechaCese, LocalDate hoy, PoliticaVacaciones politica) {
        throw new UnsupportedOperationException("pendiente");
    }
}
