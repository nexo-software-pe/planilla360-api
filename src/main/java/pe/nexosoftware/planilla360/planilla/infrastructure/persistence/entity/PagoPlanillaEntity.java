package pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity;

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
@Table(name = "pago_planilla")
public class PagoPlanillaEntity extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "planilla_id", nullable = false, updatable = false)
    private UUID planillaId;

    @Column(name = "fecha_pago", nullable = false, updatable = false)
    private LocalDate fechaPago;

    @Column(nullable = false, length = 80, updatable = false)
    private String referencia;

    protected PagoPlanillaEntity() {
    }

    public PagoPlanillaEntity(UUID planillaId, LocalDate fechaPago, String referencia) {
        this.planillaId = planillaId;
        this.fechaPago = fechaPago;
        this.referencia = referencia;
    }

    public UUID getPlanillaId() {
        return planillaId;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public String getReferencia() {
        return referencia;
    }
}
