package pe.nexosoftware.planilla360.asistencia.application.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import pe.nexosoftware.planilla360.asistencia.domain.service.ConsolidadorAsistencia;

/** Conversiones entre fechas civiles y el reloj, siempre en hora de Lima. */
final class HoraLima {

    private HoraLima() {
    }

    static LocalDate hoy(Clock reloj) {
        return reloj.instant().atZone(ConsolidadorAsistencia.ZONA_LIMA).toLocalDate();
    }

    /** Primer instante de la fecha en Lima. */
    static Instant inicioDe(LocalDate fecha) {
        return fecha.atStartOfDay(ConsolidadorAsistencia.ZONA_LIMA).toInstant();
    }
}
