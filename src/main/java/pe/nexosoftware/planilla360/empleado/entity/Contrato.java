package pe.nexosoftware.planilla360.empleado.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;
import pe.nexosoftware.planilla360.empleado.model.TipoComision;
import pe.nexosoftware.planilla360.empleado.model.TipoContrato;
import pe.nexosoftware.planilla360.empleado.model.TipoRegimenPensionario;

@Entity
@Table(name = "contrato")
public class Contrato extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "empleado_id", nullable = false, updatable = false)
    private UUID empleadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoContrato tipo;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "sueldo_basico", nullable = false, precision = 12, scale = 2)
    private BigDecimal sueldoBasico;

    @Column(name = "jornada_semanal", nullable = false)
    private int jornadaSemanal;

    @Enumerated(EnumType.STRING)
    @Column(name = "regimen_pensionario", nullable = false, length = 10)
    private TipoRegimenPensionario regimenPensionario;

    @Column(name = "afp_id")
    private UUID afpId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comision", length = 10)
    private TipoComision tipoComision;

    @Column(length = 12)
    private String cuspp;

    protected Contrato() {
    }

    public Contrato(
            UUID empleadoId,
            TipoContrato tipo,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal sueldoBasico,
            int jornadaSemanal,
            TipoRegimenPensionario regimenPensionario,
            UUID afpId,
            TipoComision tipoComision,
            String cuspp) {
        this.empleadoId = empleadoId;
        this.tipo = tipo;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.sueldoBasico = sueldoBasico;
        this.jornadaSemanal = jornadaSemanal;
        this.regimenPensionario = regimenPensionario;
        this.afpId = afpId;
        this.tipoComision = tipoComision;
        this.cuspp = cuspp == null || cuspp.isBlank() ? null : cuspp.trim();
    }

    /** Cierra el contrato el día indicado (cuando empieza uno nuevo). */
    public void cerrarEl(LocalDate fecha) {
        this.fechaFin = fecha;
    }

    public boolean sigueVigenteEn(LocalDate fecha) {
        return fechaFin == null || !fechaFin.isBefore(fecha);
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpleadoId() {
        return empleadoId;
    }

    public TipoContrato getTipo() {
        return tipo;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public BigDecimal getSueldoBasico() {
        return sueldoBasico;
    }

    public int getJornadaSemanal() {
        return jornadaSemanal;
    }

    public TipoRegimenPensionario getRegimenPensionario() {
        return regimenPensionario;
    }

    public UUID getAfpId() {
        return afpId;
    }

    public TipoComision getTipoComision() {
        return tipoComision;
    }

    public String getCuspp() {
        return cuspp;
    }
}
