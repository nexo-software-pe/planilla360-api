package pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoPermiso;
import pe.nexosoftware.planilla360.vacaciones.domain.model.TipoPermiso;
import org.hibernate.annotations.TenantId;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "permiso")
public class PermisoEntity extends AuditableEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "empleado_id", nullable = false, updatable = false)
    private UUID empleadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private TipoPermiso tipo;

    @Column(nullable = false, updatable = false)
    private LocalDate desde;

    @Column(nullable = false, updatable = false)
    private LocalDate hasta;

    @Column(name = "con_goce", nullable = false, updatable = false)
    private boolean conGoce;

    @Column(length = 300, updatable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPermiso estado;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    protected PermisoEntity() {
    }

    public PermisoEntity(
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

    public void actualizarEstado(EstadoPermiso estado, String motivoRechazo) {
        this.estado = estado;
        this.motivoRechazo = motivoRechazo;
    }

    public boolean isConGoce() {
        return conGoce;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpleadoId() {
        return empleadoId;
    }

    public TipoPermiso getTipo() {
        return tipo;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public String getMotivo() {
        return motivo;
    }

    public EstadoPermiso getEstado() {
        return estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }
}
