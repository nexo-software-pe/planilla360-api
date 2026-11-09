package pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

@Entity
@Table(name = "planilla")
public class PlanillaEntity extends AuditableEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 7, updatable = false)
    private String periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15, updatable = false)
    private TipoPlanilla tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoPlanilla estado;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "cerrada_en")
    private Instant cerradaEn;

    @Column(nullable = false)
    private int trabajadores;

    @Column(name = "total_ingresos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalIngresos = BigDecimal.ZERO;

    @Column(name = "total_descuentos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDescuentos = BigDecimal.ZERO;

    @Column(name = "total_neto", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalNeto = BigDecimal.ZERO;

    @Column(name = "total_essalud", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalEssalud = BigDecimal.ZERO;

    protected PlanillaEntity() {
    }

    public PlanillaEntity(UUID id, String periodo, TipoPlanilla tipo, EstadoPlanilla estado) {
        this.id = id;
        this.periodo = periodo;
        this.tipo = tipo;
        this.estado = estado;
    }

    public void actualizar(
            EstadoPlanilla estado,
            String idempotencyKey,
            Instant cerradaEn,
            int trabajadores,
            BigDecimal totalIngresos,
            BigDecimal totalDescuentos,
            BigDecimal totalNeto,
            BigDecimal totalEssalud) {
        this.estado = estado;
        this.idempotencyKey = idempotencyKey;
        this.cerradaEn = cerradaEn;
        this.trabajadores = trabajadores;
        this.totalIngresos = totalIngresos;
        this.totalDescuentos = totalDescuentos;
        this.totalNeto = totalNeto;
        this.totalEssalud = totalEssalud;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getPeriodo() {
        return periodo;
    }

    public TipoPlanilla getTipo() {
        return tipo;
    }

    public EstadoPlanilla getEstado() {
        return estado;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCerradaEn() {
        return cerradaEn;
    }

    public int getTrabajadores() {
        return trabajadores;
    }

    public BigDecimal getTotalIngresos() {
        return totalIngresos;
    }

    public BigDecimal getTotalDescuentos() {
        return totalDescuentos;
    }

    public BigDecimal getTotalNeto() {
        return totalNeto;
    }

    public BigDecimal getTotalEssalud() {
        return totalEssalud;
    }
}
