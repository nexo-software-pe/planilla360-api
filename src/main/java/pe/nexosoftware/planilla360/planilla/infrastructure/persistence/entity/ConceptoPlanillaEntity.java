package pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoConcepto;

/** Renglón de la boleta guardado para poder imprimirla y reportarla sin recalcular. */
@Entity
@Table(name = "concepto_planilla")
public class ConceptoPlanillaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "linea_id", nullable = false, updatable = false)
    private UUID lineaId;

    @Column(nullable = false, updatable = false)
    private int orden;

    @Column(nullable = false, length = 40, updatable = false)
    private String codigo;

    @Column(nullable = false, length = 120, updatable = false)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TipoConcepto tipo;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal monto;

    protected ConceptoPlanillaEntity() {
    }

    public ConceptoPlanillaEntity(
            UUID lineaId, int orden, String codigo, String descripcion, TipoConcepto tipo, BigDecimal monto) {
        this.lineaId = lineaId;
        this.orden = orden;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.monto = monto;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLineaId() {
        return lineaId;
    }

    public int getOrden() {
        return orden;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public TipoConcepto getTipo() {
        return tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }
}
