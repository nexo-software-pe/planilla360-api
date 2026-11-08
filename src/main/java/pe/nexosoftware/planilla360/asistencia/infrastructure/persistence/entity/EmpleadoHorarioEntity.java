package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "empleado_horario")
public class EmpleadoHorarioEntity extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "empleado_id", nullable = false, updatable = false)
    private UUID empleadoId;

    @Column(name = "horario_id", nullable = false, updatable = false)
    private UUID horarioId;

    @Column(nullable = false, updatable = false)
    private LocalDate desde;

    private LocalDate hasta;

    protected EmpleadoHorarioEntity() {
    }

    public EmpleadoHorarioEntity(UUID empleadoId, UUID horarioId, LocalDate desde) {
        this.empleadoId = empleadoId;
        this.horarioId = horarioId;
        this.desde = desde;
    }

    public void cerrarEl(LocalDate fecha) {
        this.hasta = fecha;
    }

    public UUID getEmpleadoId() {
        return empleadoId;
    }

    public UUID getHorarioId() {
        return horarioId;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }
}
