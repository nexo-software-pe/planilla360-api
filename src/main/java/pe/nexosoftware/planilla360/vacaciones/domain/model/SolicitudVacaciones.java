package pe.nexosoftware.planilla360.vacaciones.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/** Solicitud de vacaciones y su flujo de aprobación. (Esqueleto del commit ROJO.) */
public final class SolicitudVacaciones {

    public static final int BLOQUE_MINIMO_DIAS = 7;

    private final UUID id;
    private final UUID empleadoId;
    private final UUID periodoId;
    private final LocalDate desde;
    private final LocalDate hasta;
    private EstadoSolicitud estado;
    private String motivoRechazo;

    private SolicitudVacaciones(
            UUID id,
            UUID empleadoId,
            UUID periodoId,
            LocalDate desde,
            LocalDate hasta,
            EstadoSolicitud estado,
            String motivoRechazo) {
        this.id = id;
        this.empleadoId = empleadoId;
        this.periodoId = periodoId;
        this.desde = desde;
        this.hasta = hasta;
        this.estado = estado;
        this.motivoRechazo = motivoRechazo;
    }

    public static SolicitudVacaciones solicitar(
            UUID empleadoId, UUID periodoId, LocalDate desde, LocalDate hasta, int saldoDisponible, LocalDate hoy) {
        throw new UnsupportedOperationException("pendiente");
    }

    /** Reconstruye una solicitud ya guardada (lo usa el adaptador de persistencia). */
    public static SolicitudVacaciones reconstruir(
            UUID id,
            UUID empleadoId,
            UUID periodoId,
            LocalDate desde,
            LocalDate hasta,
            EstadoSolicitud estado,
            String motivoRechazo) {
        return new SolicitudVacaciones(id, empleadoId, periodoId, desde, hasta, estado, motivoRechazo);
    }

    public int dias() {
        throw new UnsupportedOperationException("pendiente");
    }

    public boolean seSolapaCon(LocalDate otroDesde, LocalDate otroHasta) {
        throw new UnsupportedOperationException("pendiente");
    }

    public void aprobarPorJefe() {
        throw new UnsupportedOperationException("pendiente");
    }

    public void aprobarPorRrhh() {
        throw new UnsupportedOperationException("pendiente");
    }

    public void rechazar(String motivo) {
        throw new UnsupportedOperationException("pendiente");
    }

    public void cancelar() {
        throw new UnsupportedOperationException("pendiente");
    }

    public void iniciarGoce(LocalDate hoy) {
        throw new UnsupportedOperationException("pendiente");
    }

    public void finalizar(LocalDate hoy) {
        throw new UnsupportedOperationException("pendiente");
    }

    public UUID id() {
        return id;
    }

    public UUID empleadoId() {
        return empleadoId;
    }

    public UUID periodoId() {
        return periodoId;
    }

    public LocalDate desde() {
        return desde;
    }

    public LocalDate hasta() {
        return hasta;
    }

    public EstadoSolicitud estado() {
        return estado;
    }

    public String motivoRechazo() {
        return motivoRechazo;
    }
}
