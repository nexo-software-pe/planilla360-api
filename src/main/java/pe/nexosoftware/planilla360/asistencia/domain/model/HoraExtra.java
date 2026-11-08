package pe.nexosoftware.planilla360.asistencia.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.domain.excepcion.ReglaDeAsistenciaException;

/**
 * Solicitud de horas extra de un empleado y su resolución. Nace {@code PENDIENTE} y se resuelve una sola
 * vez: aprobada o rechazada. Las horas van en múltiplos de un cuarto de hora, hasta 8 por solicitud.
 */
public final class HoraExtra {

    public static final BigDecimal MAXIMO_HORAS = new BigDecimal("8");
    public static final int MAXIMO_DIAS_ATRAS = 30;
    private static final BigDecimal CUARTO_DE_HORA = new BigDecimal("0.25");

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
        if (horas.signum() <= 0 || horas.compareTo(MAXIMO_HORAS) > 0) {
            throw new ReglaDeAsistenciaException("Las horas extra deben ser mayores a 0 y hasta " + MAXIMO_HORAS + ".");
        }
        if (horas.remainder(CUARTO_DE_HORA).signum() != 0) {
            throw new ReglaDeAsistenciaException("Las horas extra se registran en múltiplos de 15 minutos (0.25 h).");
        }
        if (fecha.isAfter(hoy)) {
            throw new ReglaDeAsistenciaException("No se pueden solicitar horas extra de un día futuro.");
        }
        if (fecha.isBefore(hoy.minusDays(MAXIMO_DIAS_ATRAS))) {
            throw new ReglaDeAsistenciaException(
                    "Las horas extra se solicitan dentro de los " + MAXIMO_DIAS_ATRAS + " días siguientes.");
        }
        return new HoraExtra(UUID.randomUUID(), empleadoId, fecha, horas, motivo, EstadoHoraExtra.PENDIENTE, null);
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
        resolver(aprobador, EstadoHoraExtra.APROBADA);
    }

    public void rechazar(UUID aprobador) {
        resolver(aprobador, EstadoHoraExtra.RECHAZADA);
    }

    private void resolver(UUID aprobador, EstadoHoraExtra nuevoEstado) {
        if (aprobador == null) {
            throw new ReglaDeAsistenciaException("Debe indicarse quién resuelve la solicitud.");
        }
        if (estado != EstadoHoraExtra.PENDIENTE) {
            throw new ReglaDeAsistenciaException("La solicitud ya fue resuelta (" + estado + ").");
        }
        this.estado = nuevoEstado;
        this.aprobadoPor = aprobador;
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
