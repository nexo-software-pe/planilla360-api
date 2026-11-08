package pe.nexosoftware.planilla360.vacaciones.domain.model;

import java.time.LocalDate;
import java.util.UUID;

/** Permiso o licencia de un empleado, con flujo corto de aprobación. (Esqueleto del commit ROJO.) */
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
        throw new UnsupportedOperationException("pendiente");
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
        throw new UnsupportedOperationException("pendiente");
    }

    public void aprobar() {
        throw new UnsupportedOperationException("pendiente");
    }

    public void rechazar(String motivoDelRechazo) {
        throw new UnsupportedOperationException("pendiente");
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
