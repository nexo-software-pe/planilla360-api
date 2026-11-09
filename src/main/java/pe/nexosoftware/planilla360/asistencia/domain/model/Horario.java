package pe.nexosoftware.planilla360.asistencia.domain.model;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;
import pe.nexosoftware.planilla360.asistencia.domain.excepcion.ReglaDeAsistenciaException;

/**
 * Turno de trabajo: hora de entrada y de salida, tolerancia en minutos y días laborables. Es un valor
 * inmutable: los días se copian al construirlo.
 */
public record Horario(
        LocalTime entrada, LocalTime salida, int toleranciaMin, Set<DayOfWeek> dias, int refrigerioMin) {

    public static final int TOLERANCIA_MAXIMA_MIN = 60;

    /** Horario sin refrigerio descontado. */
    public Horario(LocalTime entrada, LocalTime salida, int toleranciaMin, Set<DayOfWeek> dias) {
        this(entrada, salida, toleranciaMin, dias, 0);
    }

    public Horario {
        if (entrada == null || salida == null) {
            throw new ReglaDeAsistenciaException("El horario requiere hora de entrada y de salida.");
        }
        if (!salida.isAfter(entrada)) {
            throw new ReglaDeAsistenciaException("La hora de salida debe ser posterior a la de entrada.");
        }
        if (toleranciaMin < 0 || toleranciaMin > TOLERANCIA_MAXIMA_MIN) {
            throw new ReglaDeAsistenciaException(
                    "La tolerancia debe estar entre 0 y " + TOLERANCIA_MAXIMA_MIN + " minutos.");
        }
        if (toleranciaMin >= Duration.between(entrada, salida).toMinutes()) {
            throw new ReglaDeAsistenciaException("La tolerancia debe ser menor que la jornada.");
        }
        if (dias == null || dias.isEmpty()) {
            throw new ReglaDeAsistenciaException("El horario requiere al menos un día laborable.");
        }
        if (refrigerioMin < 0 || refrigerioMin >= Duration.between(entrada, salida).toMinutes()) {
            throw new ReglaDeAsistenciaException("El refrigerio debe ser de 0 minutos o más y menor que la jornada.");
        }
        dias = Set.copyOf(EnumSet.copyOf(dias));
    }

    /** Minutos que se trabajan por día: salida - entrada - refrigerio. */
    public int minutosEfectivos() {
        return (int) Duration.between(entrada, salida).toMinutes() - refrigerioMin;
    }

    public static Horario lunesAViernes(LocalTime entrada, LocalTime salida, int toleranciaMin) {
        return new Horario(
                entrada,
                salida,
                toleranciaMin,
                EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
    }

    public boolean trabajaEl(DayOfWeek dia) {
        return dias.contains(dia);
    }

    /** Hora máxima de llegada que aún no cuenta como tardanza. */
    public LocalTime limiteSinTardanza() {
        return entrada.plusMinutes(toleranciaMin);
    }
}
