package pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud;
import org.hibernate.annotations.TenantId;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "solicitud_vacaciones")
public class SolicitudVacacionesEntity extends AuditableEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "empleado_id", nullable = false, updatable = false)
    private UUID empleadoId;

    @Column(name = "periodo_id", nullable = false, updatable = false)
    private UUID periodoId;

    @Column(nullable = false, updatable = false)
    private LocalDate desde;

    @Column(nullable = false, updatable = false)
    private LocalDate hasta;

    @Column(nullable = false, updatable = false)
    private int dias;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    protected SolicitudVacacionesEntity() {
    }

    public SolicitudVacacionesEntity(
            UUID id,
            UUID empleadoId,
            UUID periodoId,
            LocalDate desde,
            LocalDate hasta,
            int dias,
            EstadoSolicitud estado,
            String motivoRechazo) {
        this.id = id;
        this.empleadoId = empleadoId;
        this.periodoId = periodoId;
        this.desde = desde;
        this.hasta = hasta;
        this.dias = dias;
        this.estado = estado;
        this.motivoRechazo = motivoRechazo;
    }

    public void actualizarEstado(EstadoSolicitud estado, String motivoRechazo) {
        this.estado = estado;
        this.motivoRechazo = motivoRechazo;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpleadoId() {
        return empleadoId;
    }

    public UUID getPeriodoId() {
        return periodoId;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }
}
