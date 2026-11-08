package pe.nexosoftware.planilla360.asistencia.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "marcacion")
public class MarcacionEntity extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "empleado_id", nullable = false, updatable = false)
    private UUID empleadoId;

    @Column(nullable = false, updatable = false)
    private Instant instante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private TipoMarcacion tipo;

    @Column(nullable = false, length = 20, updatable = false)
    private String origen = "WEB";

    protected MarcacionEntity() {
    }

    public MarcacionEntity(UUID empleadoId, Instant instante, TipoMarcacion tipo) {
        this.empleadoId = empleadoId;
        this.instante = instante;
        this.tipo = tipo;
    }

    public UUID getEmpleadoId() {
        return empleadoId;
    }

    public Instant getInstante() {
        return instante;
    }

    public TipoMarcacion getTipo() {
        return tipo;
    }
}
