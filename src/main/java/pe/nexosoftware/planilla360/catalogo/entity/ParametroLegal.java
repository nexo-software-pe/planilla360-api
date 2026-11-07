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

/** Valor legal (RMV, UIT, tasas…) versionado por vigencia. Nunca se hardcodea en el código. */
@Entity
@Table(name = "parametro_legal")
public class ParametroLegal extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, length = 60)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal valor;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    protected ParametroLegal() {
    }

    public ParametroLegal(String codigo, String descripcion, BigDecimal valor, LocalDate vigenteDesde) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.valor = valor;
        this.vigenteDesde = vigenteDesde;
    }

    public void cerrarVigenciaEl(LocalDate fin) {
        this.vigenteHasta = fin;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public LocalDate getVigenteDesde() {
        return vigenteDesde;
    }

    public LocalDate getVigenteHasta() {
        return vigenteHasta;
    }
}
