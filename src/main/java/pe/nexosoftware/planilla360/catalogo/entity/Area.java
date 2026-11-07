package pe.nexosoftware.planilla360.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "area")
public class Area extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    /** Lo asigna y filtra Hibernate a partir del tenant de la petición. */
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String nombre;

    protected Area() {
    }

    public Area(String nombre) {
        this.nombre = nombre.trim();
    }

    public void renombrar(String nombre) {
        this.nombre = nombre.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getNombre() {
        return nombre;
    }
}
