package pe.nexosoftware.planilla360.empresa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;
import pe.nexosoftware.planilla360.empresa.PlanSaas;
import pe.nexosoftware.planilla360.empresa.RegimenLaboral;

@Entity
@Table(name = "empresa")
public class Empresa extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Enumerated(EnumType.STRING)
    @Column(name = "regimen_laboral", nullable = false, length = 30)
    private RegimenLaboral regimenLaboral;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PlanSaas plan;

    @Column(nullable = false)
    private boolean activa = true;

    protected Empresa() {
    }

    public Empresa(String ruc, String razonSocial, RegimenLaboral regimenLaboral, PlanSaas plan) {
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.regimenLaboral = regimenLaboral;
        this.plan = plan;
    }

    public void actualizar(String razonSocial, RegimenLaboral regimenLaboral, PlanSaas plan) {
        this.razonSocial = razonSocial;
        this.regimenLaboral = regimenLaboral;
        this.plan = plan;
    }

    public void cambiarEstado(boolean activa) {
        this.activa = activa;
    }

    public UUID getId() {
        return id;
    }

    public String getRuc() {
        return ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public RegimenLaboral getRegimenLaboral() {
        return regimenLaboral;
    }

    public PlanSaas getPlan() {
        return plan;
    }

    public boolean isActiva() {
        return activa;
    }
}
