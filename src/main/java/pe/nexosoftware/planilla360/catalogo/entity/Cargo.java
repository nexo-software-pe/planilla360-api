package pe.nexosoftware.planilla360.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "cargo")
public class Cargo extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "sueldo_referencial", nullable = false, precision = 12, scale = 2)
    private BigDecimal sueldoReferencial;

    protected Cargo() {
    }

    public Cargo(UUID areaId, String nombre, BigDecimal sueldoReferencial) {
        this.areaId = areaId;
        this.nombre = nombre.trim();
        this.sueldoReferencial = sueldoReferencial;
    }

    public void actualizar(UUID areaId, String nombre, BigDecimal sueldoReferencial) {
        this.areaId = areaId;
        this.nombre = nombre.trim();
        this.sueldoReferencial = sueldoReferencial;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getAreaId() {
        return areaId;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getSueldoReferencial() {
        return sueldoReferencial;
    }
}
