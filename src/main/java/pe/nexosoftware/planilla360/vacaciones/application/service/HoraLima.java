package pe.nexosoftware.planilla360.vacaciones.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/** La fecha de hoy según el reloj, en hora de Lima. */
final class HoraLima {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private HoraLima() {
    }

    static LocalDate hoy(Clock reloj) {
        return reloj.instant().atZone(LIMA).toLocalDate();
    }
}
