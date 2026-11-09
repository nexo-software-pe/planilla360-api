package pe.nexosoftware.planilla360.reporte.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.reporte.dto.Reportes.CostoLaboral;
import pe.nexosoftware.planilla360.reporte.dto.Reportes.FilaPlanilla;
import pe.nexosoftware.planilla360.reporte.dto.Reportes.TardanzaPorArea;
import pe.nexosoftware.planilla360.reporte.dto.Reportes.VacacionPendiente;

/**
 * Reportes con SQL nativo. Las consultas NO se filtran solas por empresa (no pasan por Hibernate): todas reciben
 * {@code :tenant} y lo aplican a cada tabla que tocan. Leen de tablas de asistencia y vacaciones y de la vista
 * {@code vista_planilla_detalle} (Shared Database en solo lectura, decisión consciente del diseño).
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    /** Ranking de tardanzas por área con una CTE y {@code RANK() OVER (PARTITION BY area)}. */
    private static final String SQL_TARDANZAS = """
            WITH tardanzas AS (
                SELECT e.id AS empleado_id,
                       e.apellidos || ', ' || e.nombres AS trabajador,
                       e.area_id,
                       SUM(a.minutos_tardanza)                              AS minutos,
                       COUNT(*) FILTER (WHERE a.minutos_tardanza > 0)       AS dias
                  FROM asistencia_diaria a
                  JOIN empleado e ON e.id = a.empleado_id AND e.tenant_id = :tenant
                 WHERE a.tenant_id = :tenant
                   AND a.fecha BETWEEN :desde AND :hasta
                 GROUP BY e.id, e.apellidos, e.nombres, e.area_id
                HAVING SUM(a.minutos_tardanza) > 0
            ), ranking AS (
                SELECT ar.nombre AS area, t.trabajador, t.minutos, t.dias,
                       RANK() OVER (PARTITION BY t.area_id ORDER BY t.minutos DESC) AS posicion
                  FROM tardanzas t
                  JOIN area ar ON ar.id = t.area_id AND ar.tenant_id = :tenant
            )
            SELECT area, trabajador, minutos, dias, posicion
              FROM ranking
             WHERE posicion <= :top
             ORDER BY area, posicion, trabajador
            """;

    /** Costo laboral con subtotales por área y total general ({@code ROLLUP}) sobre la vista de planillas cerradas. */
    private static final String SQL_COSTO_LABORAL = """
            SELECT CASE WHEN GROUPING(area) = 1 THEN 'EMPRESA'
                        WHEN GROUPING(cargo) = 1 THEN 'AREA'
                        ELSE 'CARGO' END                               AS nivel,
                   area, cargo,
                   COUNT(*)                                            AS trabajadores,
                   SUM(total_ingresos)                                 AS ingresos,
                   SUM(aporte_essalud)                                 AS essalud,
                   SUM(total_ingresos + aporte_essalud)                AS costo_total
              FROM vista_planilla_detalle
             WHERE tenant_id = :tenant AND periodo = :periodo AND tipo = 'MENSUAL'
             GROUP BY ROLLUP (area, cargo)
            HAVING COUNT(*) > 0 -- sin planilla cerrada, ROLLUP devolvería un total vacío
             ORDER BY GROUPING(area), area, GROUPING(cargo), cargo
            """;

    /** Saldos por periodo vacacional y saldo total del trabajador con una función de ventana. */
    private static final String SQL_VACACIONES = """
            WITH saldos AS (
                SELECT p.empleado_id, p.desde, p.hasta,
                       p.dias_ganados - p.dias_gozados - p.dias_vendidos AS saldo
                  FROM periodo_vacacional p
                 WHERE p.tenant_id = :tenant
            )
            SELECT e.id AS empleado_id,
                   e.apellidos || ', ' || e.nombres AS trabajador,
                   s.desde, s.hasta, s.saldo,
                   SUM(s.saldo) OVER (PARTITION BY s.empleado_id) AS saldo_total,
                   s.hasta < :hoy                                  AS vencido
              FROM saldos s
              JOIN empleado e ON e.id = s.empleado_id AND e.tenant_id = :tenant
             WHERE s.saldo > 0
               AND e.fecha_cese IS NULL
             ORDER BY saldo_total DESC, trabajador, s.desde
            """;

    private static final String SQL_PLANILLA = """
            SELECT dni, trabajador, area, cargo, basico, asig_familiar, horas_extra, gratificacion, cts,
                   total_ingresos, total_descuentos, neto, aporte_essalud
              FROM vista_planilla_detalle
             WHERE tenant_id = :tenant AND periodo = :periodo AND tipo = :tipo
             ORDER BY area, trabajador
            """;

    private final JdbcClient jdbc;
    private final Clock reloj;

    public ReporteService(JdbcClient jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    public List<TardanzaPorArea> tardanzasPorArea(YearMonth periodo, int top) {
        return jdbc.sql(SQL_TARDANZAS)
                .param("tenant", tenant())
                .param("desde", periodo.atDay(1))
                .param("hasta", periodo.atEndOfMonth())
                .param("top", top)
                .query((rs, n) -> new TardanzaPorArea(
                        rs.getString("area"),
                        rs.getString("trabajador"),
                        rs.getLong("minutos"),
                        rs.getLong("dias"),
                        rs.getInt("posicion")))
                .list();
    }

    public List<CostoLaboral> costoLaboral(YearMonth periodo) {
        return jdbc.sql(SQL_COSTO_LABORAL)
                .param("tenant", tenant())
                .param("periodo", periodo.toString())
                .query((rs, n) -> new CostoLaboral(
                        rs.getString("nivel"),
                        rs.getString("area"),
                        rs.getString("cargo"),
                        rs.getLong("trabajadores"),
                        rs.getBigDecimal("ingresos"),
                        rs.getBigDecimal("essalud"),
                        rs.getBigDecimal("costo_total")))
                .list();
    }

    public List<VacacionPendiente> vacacionesPendientes() {
        LocalDate hoy = LocalDate.now(reloj.withZone(LIMA));
        return jdbc.sql(SQL_VACACIONES)
                .param("tenant", tenant())
                .param("hoy", hoy)
                .query((rs, n) -> new VacacionPendiente(
                        rs.getObject("empleado_id", UUID.class),
                        rs.getString("trabajador"),
                        rs.getObject("desde", LocalDate.class),
                        rs.getObject("hasta", LocalDate.class),
                        rs.getInt("saldo"),
                        rs.getInt("saldo_total"),
                        rs.getBoolean("vencido")))
                .list();
    }

    public List<FilaPlanilla> planilla(YearMonth periodo, String tipo) {
        return jdbc.sql(SQL_PLANILLA)
                .param("tenant", tenant())
                .param("periodo", periodo.toString())
                .param("tipo", tipo)
                .query((rs, n) -> new FilaPlanilla(
                        rs.getString("dni"),
                        rs.getString("trabajador"),
                        rs.getString("area"),
                        rs.getString("cargo"),
                        rs.getBigDecimal("basico"),
                        rs.getBigDecimal("asig_familiar"),
                        rs.getBigDecimal("horas_extra"),
                        rs.getBigDecimal("gratificacion"),
                        rs.getBigDecimal("cts"),
                        rs.getBigDecimal("total_ingresos"),
                        rs.getBigDecimal("total_descuentos"),
                        rs.getBigDecimal("neto"),
                        rs.getBigDecimal("aporte_essalud")))
                .list();
    }

    private static UUID tenant() {
        return TenantContext.requerido();
    }
}
