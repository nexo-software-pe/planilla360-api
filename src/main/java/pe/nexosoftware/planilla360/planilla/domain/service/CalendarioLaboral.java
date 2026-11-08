package pe.nexosoftware.planilla360.planilla.domain.service;

import java.time.LocalDate;
import java.util.function.Predicate;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;

/** Días hábiles del mes: de lunes a viernes y que no sean feriado. */
public final class CalendarioLaboral {

    /** Último día hábil del periodo. */
    public LocalDate ultimoDiaHabil(Periodo periodo, Predicate<LocalDate> esFeriado) {
        throw new UnsupportedOperationException("pendiente");
    }

    public boolean esUltimoDiaHabil(LocalDate fecha, Predicate<LocalDate> esFeriado) {
        throw new UnsupportedOperationException("pendiente");
    }
}
