package pe.nexosoftware.planilla360.catalogo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "feriado")
public class Feriado extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 120)
    private String descripcion;

    @Column(nullable = false, length = 20)
    private String ambito;

    protected Feriado() {
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getAmbito() {
        return ambito;
    }
}
