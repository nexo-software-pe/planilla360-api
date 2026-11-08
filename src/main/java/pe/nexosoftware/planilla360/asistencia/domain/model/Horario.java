package pe.nexosoftware.planilla360.asistencia.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/** Turno de trabajo: entrada, salida, tolerancia en minutos y días laborables. (Esqueleto del commit ROJO.) */
public record Horario(LocalTime entrada, LocalTime salida, int toleranciaMin, Set<DayOfWeek> dias) {

    public Horario {
        throw new UnsupportedOperationException("pendiente");
    }

    public static Horario lunesAViernes(LocalTime entrada, LocalTime salida, int toleranciaMin) {
        throw new UnsupportedOperationException("pendiente");
    }

    public boolean trabajaEl(DayOfWeek dia) {
        throw new UnsupportedOperationException("pendiente");
    }

    public LocalTime limiteSinTardanza() {
        throw new UnsupportedOperationException("pendiente");
    }
}
