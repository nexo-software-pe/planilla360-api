package pe.nexosoftware.planilla360.vacaciones.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "periodo_vacacional")
public class PeriodoVacacionalEntity extends AuditableEntity {

    @Id
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "empleado_id", nullable = false, updatable = false)
    private UUID empleadoId;

    @Column(nullable = false, updatable = false)
    private LocalDate desde;

    @Column(nullable = false, updatable = false)
    private LocalDate hasta;

    @Column(name = "dias_ganados", nullable = false, updatable = false)
    private int diasGanados;

    @Column(name = "dias_gozados", nullable = false)
    private int diasGozados;

    @Column(name = "dias_vendidos", nullable = false)
    private int diasVendidos;

    protected PeriodoVacacionalEntity() {
    }

    public PeriodoVacacionalEntity(
            UUID id,
            UUID empleadoId,
            LocalDate desde,
            LocalDate hasta,
            int diasGanados,
            int diasGozados,
            int diasVendidos) {
        this.id = id;
        this.empleadoId = empleadoId;
        this.desde = desde;
        this.hasta = hasta;
        this.diasGanados = diasGanados;
        this.diasGozados = diasGozados;
        this.diasVendidos = diasVendidos;
    }

    public void actualizarDias(int diasGozados, int diasVendidos) {
        this.diasGozados = diasGozados;
        this.diasVendidos = diasVendidos;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEmpleadoId() {
        return empleadoId;
    }

    public LocalDate getDesde() {
        return desde;
    }

    public LocalDate getHasta() {
        return hasta;
    }

    public int getDiasGanados() {
        return diasGanados;
    }

    public int getDiasGozados() {
        return diasGozados;
    }

    public int getDiasVendidos() {
        return diasVendidos;
    }
}
