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
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;
import pe.nexosoftware.planilla360.planilla.domain.regimen.TipoRegimenPensionario;

/** Línea de planilla (boleta de un trabajador): guarda todos los componentes para reconstruir el dominio sin recalcular. */
@Entity
@Table(name = "linea_planilla")
public class LineaPlanillaEntity extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "planilla_id", nullable = false, updatable = false)
    private UUID planillaId;

    @Column(name = "empleado_id", nullable = false, updatable = false)
    private UUID empleadoId;

    @Column(nullable = false, updatable = false)
    private int orden;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basico;

    @Column(name = "asig_familiar", nullable = false, precision = 12, scale = 2)
    private BigDecimal asignacionFamiliar;

    @Column(name = "horas_extra", nullable = false, precision = 12, scale = 2)
    private BigDecimal horasExtra;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal bonos;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal gratificacion;

    @Column(name = "bonif_extra", nullable = false, precision = 12, scale = 2)
    private BigDecimal bonificacionExtraordinaria;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cts;

    @Enumerated(EnumType.STRING)
    @Column(name = "regimen_pensionario", nullable = false, length = 10)
    private TipoRegimenPensionario regimenPensionario;

    @Column(name = "afp_nombre", length = 60)
    private String afpNombre;

    @Column(name = "afp_comision_pct", precision = 7, scale = 4)
    private BigDecimal afpComisionPct;

    @Column(name = "pension_aporte", nullable = false, precision = 12, scale = 2)
    private BigDecimal pensionAporte;

    @Column(name = "pension_prima", nullable = false, precision = 12, scale = 2)
    private BigDecimal pensionPrima;

    @Column(name = "pension_comision", nullable = false, precision = 12, scale = 2)
    private BigDecimal pensionComision;

    @Column(name = "retencion_5ta", nullable = false, precision = 12, scale = 2)
    private BigDecimal retencion5ta;

    @Column(name = "descuento_asistencia", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuentoAsistencia;

    @Column(name = "otros_descuentos", nullable = false, precision = 12, scale = 2)
    private BigDecimal otrosDescuentos;

    @Column(name = "aporte_essalud", nullable = false, precision = 12, scale = 2)
    private BigDecimal aporteEssalud;

    @Column(name = "total_ingresos", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIngresos;

    @Column(name = "total_descuentos", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDescuentos;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal neto;

    protected LineaPlanillaEntity() {
    }

    /** Montos ya calculados por el dominio, en el orden de la boleta; la entidad solo los guarda. */
    public record Importes(
            BigDecimal basico,
            BigDecimal asignacionFamiliar,
            BigDecimal horasExtra,
            BigDecimal bonos,
            BigDecimal gratificacion,
            BigDecimal bonificacionExtraordinaria,
            BigDecimal cts,
            BigDecimal pensionAporte,
            BigDecimal pensionPrima,
            BigDecimal pensionComision,
            BigDecimal retencion5ta,
            BigDecimal descuentoAsistencia,
            BigDecimal otrosDescuentos,
            BigDecimal aporteEssalud,
            BigDecimal totalIngresos,
            BigDecimal totalDescuentos,
            BigDecimal neto) {
    }

    public LineaPlanillaEntity(
            UUID planillaId,
            UUID empleadoId,
            int orden,
            Importes importes,
            TipoRegimenPensionario regimenPensionario,
            String afpNombre,
            BigDecimal afpComisionPct) {
        this.planillaId = planillaId;
        this.empleadoId = empleadoId;
        this.orden = orden;
        this.regimenPensionario = regimenPensionario;
        this.afpNombre = afpNombre;
        this.afpComisionPct = afpComisionPct;
        this.basico = importes.basico();
        this.asignacionFamiliar = importes.asignacionFamiliar();
        this.horasExtra = importes.horasExtra();
        this.bonos = importes.bonos();
        this.gratificacion = importes.gratificacion();
        this.bonificacionExtraordinaria = importes.bonificacionExtraordinaria();
        this.cts = importes.cts();
        this.pensionAporte = importes.pensionAporte();
        this.pensionPrima = importes.pensionPrima();
        this.pensionComision = importes.pensionComision();
        this.retencion5ta = importes.retencion5ta();
        this.descuentoAsistencia = importes.descuentoAsistencia();
        this.otrosDescuentos = importes.otrosDescuentos();
        this.aporteEssalud = importes.aporteEssalud();
        this.totalIngresos = importes.totalIngresos();
        this.totalDescuentos = importes.totalDescuentos();
        this.neto = importes.neto();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlanillaId() {
        return planillaId;
    }

    public UUID getEmpleadoId() {
        return empleadoId;
    }

    public int getOrden() {
        return orden;
    }

    public BigDecimal getBasico() {
        return basico;
    }

    public BigDecimal getAsignacionFamiliar() {
        return asignacionFamiliar;
    }

    public BigDecimal getHorasExtra() {
        return horasExtra;
    }

    public BigDecimal getBonos() {
        return bonos;
    }

    public BigDecimal getGratificacion() {
        return gratificacion;
    }

    public BigDecimal getBonificacionExtraordinaria() {
        return bonificacionExtraordinaria;
    }

    public BigDecimal getCts() {
        return cts;
    }

    public TipoRegimenPensionario getRegimenPensionario() {
        return regimenPensionario;
    }

    public String getAfpNombre() {
        return afpNombre;
    }

    public BigDecimal getAfpComisionPct() {
        return afpComisionPct;
    }

    public BigDecimal getPensionAporte() {
        return pensionAporte;
    }

    public BigDecimal getPensionPrima() {
        return pensionPrima;
    }

    public BigDecimal getPensionComision() {
        return pensionComision;
    }

    public BigDecimal getRetencion5ta() {
        return retencion5ta;
    }

    public BigDecimal getDescuentoAsistencia() {
        return descuentoAsistencia;
    }

    public BigDecimal getOtrosDescuentos() {
        return otrosDescuentos;
    }

    public BigDecimal getAporteEssalud() {
        return aporteEssalud;
    }

    public BigDecimal getTotalIngresos() {
        return totalIngresos;
    }

    public BigDecimal getTotalDescuentos() {
        return totalDescuentos;
    }

    public BigDecimal getNeto() {
        return neto;
    }
}
