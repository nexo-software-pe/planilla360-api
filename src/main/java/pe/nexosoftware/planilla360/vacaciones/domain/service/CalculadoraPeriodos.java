package pe.nexosoftware.planilla360.vacaciones.domain.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pe.nexosoftware.planilla360.vacaciones.domain.model.PoliticaVacaciones;

/**
 * Determina los periodos vacacionales ganados por un empleado. Simplificación del negocio: se gana un
 * periodo al cumplir cada año desde la fecha de ingreso (no se cuenta el récord de 260 o 210 días
 * efectivos). Un empleado cesado no gana periodos después de su cese.
 */
public final class CalculadoraPeriodos {

    /** Un año de servicio completo y los días que otorgó. */
    public record PeriodoCalculado(LocalDate desde, LocalDate hasta, int diasGanados) {
    }

    public List<PeriodoCalculado> periodosCumplidos(
            LocalDate fechaIngreso, LocalDate fechaCese, LocalDate hoy, PoliticaVacaciones politica) {
        LocalDate referencia = fechaCese != null && fechaCese.isBefore(hoy) ? fechaCese : hoy;
        List<PeriodoCalculado> periodos = new ArrayList<>();
        for (int anio = 1; !fechaIngreso.plusYears(anio).isAfter(referencia); anio++) {
            LocalDate desde = fechaIngreso.plusYears(anio - 1L);
            LocalDate aniversario = fechaIngreso.plusYears(anio);
            periodos.add(new PeriodoCalculado(desde, aniversario.minusDays(1), politica.diasPorAnio()));
        }
        return periodos;
    }
}
