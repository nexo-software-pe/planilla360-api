package pe.nexosoftware.planilla360.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "afp")
public class Afp extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "comision_flujo", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionFlujo;

    @Column(name = "comision_mixta", nullable = false, precision = 5, scale = 2)
    private BigDecimal comisionMixta;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    protected Afp() {
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getComisionFlujo() {
        return comisionFlujo;
    }

    public BigDecimal getComisionMixta() {
        return comisionMixta;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }
}
