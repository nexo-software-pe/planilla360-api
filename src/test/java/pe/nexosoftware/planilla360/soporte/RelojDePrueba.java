package pe.nexosoftware.planilla360.soporte;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Reloj que las pruebas pueden fijar en un instante. Sin fijar, se comporta como el reloj del sistema.
 * Las pruebas usan fechas futuras (2030) para que los JWT firmados con este reloj sigan siendo válidos
 * para el decodificador, que valida la expiración con la hora real.
 */
public class RelojDePrueba extends Clock {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private volatile Instant fijo;

    public void fijarEnLima(String fechaHora) {
        this.fijo = LocalDateTime.parse(fechaHora).atZone(LIMA).toInstant();
    }

    public void restablecer() {
        this.fijo = null;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        Instant actual = fijo;
        return actual != null ? actual : Instant.now();
    }
}
