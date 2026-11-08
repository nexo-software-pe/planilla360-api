package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoAsistencia;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "asistencia_diaria")
public class AsistenciaDiariaEntity extends AuditableEntity {

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

    @Column(name = "minutos_tardanza", nullable = false)
    private int minutosTardanza;

    @Column(name = "minutos_extra", nullable = false)
    private int minutosExtra;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsistencia estado;

    protected AsistenciaDiariaEntity() {
    }

    public AsistenciaDiariaEntity(
            UUID empleadoId, LocalDate fecha, int minutosTardanza, int minutosExtra, EstadoAsistencia estado) {
        this.empleadoId = empleadoId;
        this.fecha = fecha;
        reemplazarCon(minutosTardanza, minutosExtra, estado);
    }

    public void reemplazarCon(int minutosTardanza, int minutosExtra, EstadoAsistencia estado) {
        this.minutosTardanza = minutosTardanza;
        this.minutosExtra = minutosExtra;
        this.estado = estado;
    }

    public UUID getEmpleadoId() {
        return empleadoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public int getMinutosTardanza() {
        return minutosTardanza;
    }

    public int getMinutosExtra() {
        return minutosExtra;
    }

    public EstadoAsistencia getEstado() {
        return estado;
    }
}
