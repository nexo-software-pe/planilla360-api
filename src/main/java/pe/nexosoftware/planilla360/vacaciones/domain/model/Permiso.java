package pe.nexosoftware.planilla360.vacaciones.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.domain.excepcion.ReglaDeVacacionesException;

/** Permiso o licencia de un empleado. Flujo corto: nace solicitado y se aprueba o rechaza una sola vez. */
public final class Permiso {

    public static final int MAXIMO_DIAS = 90;

    private final UUID id;
    private final UUID empleadoId;
    private final TipoPermiso tipo;
    private final LocalDate desde;
    private final LocalDate hasta;
    private final boolean conGoce;
    private final String motivo;
    private EstadoPermiso estado;
    private String motivoRechazo;

    private Permiso(
            UUID id,
            UUID empleadoId,
            TipoPermiso tipo,
            LocalDate desde,
            LocalDate hasta,
            boolean conGoce,
            String motivo,
            EstadoPermiso estado,
            String motivoRechazo) {
        this.id = id;
        this.empleadoId = empleadoId;
        this.tipo = tipo;
        this.desde = desde;
        this.hasta = hasta;
        this.conGoce = conGoce;
        this.motivo = motivo;
        this.estado = estado;
        this.motivoRechazo = motivoRechazo;
    }

    public static Permiso solicitar(
            UUID empleadoId, TipoPermiso tipo, LocalDate desde, LocalDate hasta, boolean conGoce, String motivo) {
        if (hasta.isBefore(desde)) {
            throw new ReglaDeVacacionesException("La fecha final no puede ser anterior a la fecha de inicio.");
        }
        Permiso permiso = new Permiso(
                UUID.randomUUID(), empleadoId, tipo, desde, hasta, conGoce, motivo, EstadoPermiso.SOLICITADO, null);
        if (permiso.dias() > MAXIMO_DIAS) {
            throw new ReglaDeVacacionesException("Un permiso no puede durar más de " + MAXIMO_DIAS + " días.");
        }
        return permiso;
    }

    /** Reconstruye un permiso ya guardado (lo usa el adaptador de persistencia). */
    public static Permiso reconstruir(
            UUID id,
            UUID empleadoId,
            TipoPermiso tipo,
            LocalDate desde,
            LocalDate hasta,
            boolean conGoce,
            String motivo,
            EstadoPermiso estado,
            String motivoRechazo) {
        return new Permiso(id, empleadoId, tipo, desde, hasta, conGoce, motivo, estado, motivoRechazo);
    }

    public int dias() {
        return (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    public void aprobar() {
        exigirSolicitado();
        this.estado = EstadoPermiso.APROBADO;
    }

    public void rechazar(String motivoDelRechazo) {
        if (motivoDelRechazo == null || motivoDelRechazo.isBlank()) {
            throw new ReglaDeVacacionesException("Debe indicarse el motivo del rechazo.");
        }
        exigirSolicitado();
        this.estado = EstadoPermiso.RECHAZADO;
        this.motivoRechazo = motivoDelRechazo.trim();
    }

    private void exigirSolicitado() {
        if (estado != EstadoPermiso.SOLICITADO) {
            throw new ReglaDeVacacionesException("El permiso ya fue resuelto (" + estado + ").");
        }
    }

    public UUID id() {
        return id;
    }

    public UUID empleadoId() {
        return empleadoId;
    }

    public TipoPermiso tipo() {
        return tipo;
    }

    public LocalDate desde() {
        return desde;
    }

    public LocalDate hasta() {
        return hasta;
    }

    public boolean conGoce() {
        return conGoce;
    }

    public String motivo() {
        return motivo;
    }

    public EstadoPermiso estado() {
        return estado;
    }

    public String motivoRechazo() {
        return motivoRechazo;
    }
}
