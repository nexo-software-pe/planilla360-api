package pe.nexosoftware.planilla360.planilla.domain.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.function.Predicate;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;

/** Días hábiles del mes: de lunes a viernes y que no sean feriado. */
public final class CalendarioLaboral {

    /** Último día hábil del periodo. */
    public LocalDate ultimoDiaHabil(Periodo periodo, Predicate<LocalDate> esFeriado) {
        for (LocalDate dia = periodo.fin(); !dia.isBefore(periodo.inicio()); dia = dia.minusDays(1)) {
            if (esHabil(dia, esFeriado)) {
                return dia;
            }
        }
        throw new ReglaDePlanillaException("El periodo " + periodo + " no tiene ningún día hábil");
    }

    public boolean esUltimoDiaHabil(LocalDate fecha, Predicate<LocalDate> esFeriado) {
        return ultimoDiaHabil(Periodo.de(fecha), esFeriado).equals(fecha);
    }

    private static boolean esHabil(LocalDate dia, Predicate<LocalDate> esFeriado) {
        DayOfWeek semana = dia.getDayOfWeek();
        return semana != DayOfWeek.SATURDAY && semana != DayOfWeek.SUNDAY && !esFeriado.test(dia);
    }
}
