package pe.nexosoftware.planilla360.planilla;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Evento público del módulo: una planilla quedó cerrada. Es la señal para generar las boletas y avisar a los
 * trabajadores. Se publica una sola vez por cierre (repetir el cierre con la misma clave no lo vuelve a publicar).
 *
 * @param tipo MENSUAL, GRATIFICACION o CTS
 */
public record PlanillaCerradaEvento(
        UUID planillaId,
        UUID empresaId,
        YearMonth periodo,
        String tipo,
        Instant cerradaEn,
        int trabajadores,
        BigDecimal totalNeto) {
}
