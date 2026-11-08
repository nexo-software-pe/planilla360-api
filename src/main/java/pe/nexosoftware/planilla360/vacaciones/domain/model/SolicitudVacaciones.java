package pe.nexosoftware.planilla360.vacaciones.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.domain.excepcion.ReglaDeVacacionesException;

/**
 * Solicitud de vacaciones y su flujo de aprobación: el jefe aprueba primero, luego RRHH. Los días se
 * cuentan en calendario, incluyendo ambos extremos. Los bloques deben ser de al menos 7 días, salvo que
 * se tome todo el saldo restante. Una solicitud ya aprobada por RRHH no se cancela (ya generó días
 * justificados en asistencia).
 */
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
        if (hasta.isBefore(desde)) {
            throw new ReglaDeVacacionesException("La fecha final no puede ser anterior a la fecha de inicio.");
        }
        if (desde.isBefore(hoy)) {
            throw new ReglaDeVacacionesException("Las vacaciones no pueden empezar en el pasado.");
        }
        SolicitudVacaciones solicitud = new SolicitudVacaciones(
                UUID.randomUUID(), empleadoId, periodoId, desde, hasta, EstadoSolicitud.SOLICITADA, null);
        int dias = solicitud.dias();
        if (dias > saldoDisponible) {
            throw new ReglaDeVacacionesException(
                    "Solicitas " + dias + " días pero tu saldo disponible es de " + saldoDisponible + ".");
        }
        if (dias < BLOQUE_MINIMO_DIAS && dias != saldoDisponible) {
            throw new ReglaDeVacacionesException(
                    "Cada bloque de vacaciones debe ser de al menos " + BLOQUE_MINIMO_DIAS + " días.");
        }
        return solicitud;
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
        return (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    public boolean seSolapaCon(LocalDate otroDesde, LocalDate otroHasta) {
        return !otroDesde.isAfter(hasta) && !otroHasta.isBefore(desde);
    }

    public void aprobarPorJefe() {
        pasarA(EstadoSolicitud.APROBADA_JEFE);
    }

    public void aprobarPorRrhh() {
        pasarA(EstadoSolicitud.APROBADA_RRHH);
    }

    public void rechazar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeVacacionesException("Debe indicarse el motivo del rechazo.");
        }
        pasarA(EstadoSolicitud.RECHAZADA);
        this.motivoRechazo = motivo.trim();
    }

    public void cancelar() {
        pasarA(EstadoSolicitud.CANCELADA);
    }

    public void iniciarGoce(LocalDate hoy) {
        if (hoy.isBefore(desde)) {
            throw new ReglaDeVacacionesException("El goce empieza el " + desde + ".");
        }
        pasarA(EstadoSolicitud.EN_GOCE);
    }

    public void finalizar(LocalDate hoy) {
        if (!hoy.isAfter(hasta)) {
            throw new ReglaDeVacacionesException("El goce termina el " + hasta + ".");
        }
        pasarA(EstadoSolicitud.FINALIZADA);
    }

    private void pasarA(EstadoSolicitud destino) {
        if (!estado.puedePasarA(destino)) {
            throw new ReglaDeVacacionesException("No se puede pasar de " + estado + " a " + destino + ".");
        }
        this.estado = destino;
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
