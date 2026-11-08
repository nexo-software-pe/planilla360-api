package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoHoraExtra;
import org.hibernate.annotations.TenantId;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "hora_extra")
public class HoraExtraEntity extends AuditableEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "empleado_id", nullable = false, updatable = false)
    private UUID empleadoId;

    @Column(nullable = false, updatable = false)
    private LocalDate fecha;

    @Column(nullable = false, precision = 4, scale = 2, updatable = false)
    private BigDecimal horas;

    @Column(length = 200, updatable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoHoraExtra estado;

    @Column(name = "aprobado_por")
    private UUID aprobadoPor;

    protected HoraExtraEntity() {
    }

    public HoraExtraEntity(
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

    public void resolverCon(EstadoHoraExtra estado, UUID aprobadoPor) {
        this.estado = estado;
        this.aprobadoPor = aprobadoPor;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpleadoId() {
        return empleadoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public BigDecimal getHoras() {
        return horas;
    }

    public String getMotivo() {
        return motivo;
    }

    public EstadoHoraExtra getEstado() {
        return estado;
    }

    public UUID getAprobadoPor() {
        return aprobadoPor;
    }
}
