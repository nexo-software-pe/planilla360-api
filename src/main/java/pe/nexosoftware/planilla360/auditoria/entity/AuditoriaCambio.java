package pe.nexosoftware.planilla360.auditoria.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;

/** Historial inmutable de cambios. {@code tenantId} nulo = cambio de alcance plataforma. */
@Entity
@Table(name = "auditoria_cambio")
public class AuditoriaCambio {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false, length = 60)
    private String entidad;

    @Column(name = "entidad_id", nullable = false)
    private UUID entidadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Accion accion;

    @Column(nullable = false, length = 160)
    private String usuario;

    @Column(nullable = false)
    private Instant instante;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String antes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String despues;

    protected AuditoriaCambio() {
    }

    public AuditoriaCambio(
            UUID tenantId, String entidad, UUID entidadId, Accion accion, String usuario, Instant instante,
            String antes, String despues) {
        this.tenantId = tenantId;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.accion = accion;
        this.usuario = usuario;
        this.instante = instante;
        this.antes = antes;
        this.despues = despues;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEntidad() {
        return entidad;
    }

    public UUID getEntidadId() {
        return entidadId;
    }

    public Accion getAccion() {
        return accion;
    }

    public String getUsuario() {
        return usuario;
    }

    public Instant getInstante() {
        return instante;
    }

    public String getAntes() {
        return antes;
    }

    public String getDespues() {
        return despues;
    }
}
