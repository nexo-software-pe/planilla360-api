package pe.nexosoftware.planilla360.asistencia.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Solicitud de horas extra de un empleado y su aprobación. (Esqueleto del commit ROJO.) */
public final class HoraExtra {

    public static final BigDecimal MAXIMO_HORAS = new BigDecimal("8");
    public static final int MAXIMO_DIAS_ATRAS = 30;

    private final UUID id;
    private final UUID empleadoId;
    private final LocalDate fecha;
    private final BigDecimal horas;
    private final String motivo;
    private EstadoHoraExtra estado;
    private UUID aprobadoPor;

    private HoraExtra(
            UUID id,
            UUID empleadoId,
            LocalDate fecha,
            BigDecimal horas,
            String motivo,
            EstadoHoraExtra estado,
            UUID aprobadoPor) {
        this.id = id;
        this.empleadoId = empleadoId;
        this.fecha = fecha;
        this.horas = horas;
        this.motivo = motivo;
        this.estado = estado;
        this.aprobadoPor = aprobadoPor;
    }

    public static HoraExtra solicitar(
            UUID empleadoId, LocalDate fecha, BigDecimal horas, String motivo, LocalDate hoy) {
        throw new UnsupportedOperationException("pendiente");
    }

    /** Reconstruye una solicitud ya guardada (lo usa el adaptador de persistencia). */
    public static HoraExtra reconstruir(
            UUID id,
            UUID empleadoId,
            LocalDate fecha,
            BigDecimal horas,
            String motivo,
            EstadoHoraExtra estado,
            UUID aprobadoPor) {
        return new HoraExtra(id, empleadoId, fecha, horas, motivo, estado, aprobadoPor);
    }

    public void aprobar(UUID aprobador) {
        throw new UnsupportedOperationException("pendiente");
    }

    public void rechazar(UUID aprobador) {
        throw new UnsupportedOperationException("pendiente");
    }

    public UUID id() {
        return id;
    }

    public UUID empleadoId() {
        return empleadoId;
    }

    public LocalDate fecha() {
        return fecha;
    }

    public BigDecimal horas() {
        return horas;
    }

    public String motivo() {
        return motivo;
    }

    public EstadoHoraExtra estado() {
        return estado;
    }

    public UUID aprobadoPor() {
        return aprobadoPor;
    }
}
