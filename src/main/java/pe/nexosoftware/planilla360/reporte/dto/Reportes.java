package pe.nexosoftware.planilla360.reporte.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Filas de los reportes (solo lectura). */
public final class Reportes {

    private Reportes() {
    }

    /** @param posicion lugar del trabajador dentro de su área (1 = más minutos de tardanza) */
    public record TardanzaPorArea(String area, String trabajador, long minutos, long dias, int posicion) {
    }

    /** @param nivel CARGO, AREA (subtotal) o EMPRESA (total general) */
    public record CostoLaboral(
            String nivel,
            String area,
            String cargo,
            long trabajadores,
            BigDecimal ingresos,
            BigDecimal essalud,
            BigDecimal costoTotal) {
    }

    /** @param saldoTotal suma de los saldos de todos los periodos del trabajador (función de ventana) */
    public record VacacionPendiente(
            UUID empleadoId,
            String trabajador,
            LocalDate periodoDesde,
            LocalDate periodoHasta,
            int saldoDias,
            int saldoTotalDias,
            boolean vencido) {
    }

    public record FilaPlanilla(
            String dni,
            String trabajador,
            String area,
            String cargo,
            BigDecimal basico,
            BigDecimal asignacionFamiliar,
            BigDecimal horasExtra,
            BigDecimal gratificacion,
            BigDecimal cts,
            BigDecimal totalIngresos,
            BigDecimal totalDescuentos,
            BigDecimal neto,
            BigDecimal essalud) {
    }
}
