package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.application.port.in.JustificarDiasUseCase.Motivo;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "dia_justificado")
public class DiaJustificadoEntity extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "empleado_id", nullable = false, updatable = false)
    private UUID empleadoId;

    @Column(nullable = false, updatable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private Motivo motivo;

    @Column(name = "origen_id", nullable = false, updatable = false)
    private UUID origenId;

    protected DiaJustificadoEntity() {
    }

    public DiaJustificadoEntity(UUID empleadoId, LocalDate fecha, Motivo motivo, UUID origenId) {
        this.empleadoId = empleadoId;
        this.fecha = fecha;
        this.motivo = motivo;
        this.origenId = origenId;
    }

    public UUID getEmpleadoId() {
        return empleadoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }
}
