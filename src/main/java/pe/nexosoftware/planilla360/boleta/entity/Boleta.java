package pe.nexosoftware.planilla360.boleta.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.boleta.model.EstadoBoleta;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;

@Entity
@Table(name = "boleta")
public class Boleta extends AuditableEntity {

    private static final int LARGO_MAXIMO_ERROR = 300;

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

    @Column(nullable = false, updatable = false, length = 7)
    private String periodo;

    @Column(nullable = false, updatable = false, length = 15)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoBoleta estado = EstadoBoleta.PENDIENTE;

    @Column(name = "archivo_clave", length = 300)
    private String archivoClave;

    @Column(length = 64)
    private String hash;

    @Column(name = "tamano_bytes")
    private Integer tamanoBytes;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "ultimo_error", length = LARGO_MAXIMO_ERROR)
    private String ultimoError;

    @Column(name = "generada_en")
    private Instant generadaEn;

    @Column(name = "enviada_en")
    private Instant enviadaEn;

    protected Boleta() {
    }

    public Boleta(UUID planillaId, UUID empleadoId, String periodo, String tipo) {
        this.planillaId = planillaId;
        this.empleadoId = empleadoId;
        this.periodo = periodo;
        this.tipo = tipo;
    }

    public void registrarGeneracion(String clave, String hashSha256, int tamano, Instant ahora) {
        this.estado = EstadoBoleta.GENERADA;
        this.archivoClave = clave;
        this.hash = hashSha256;
        this.tamanoBytes = tamano;
        this.generadaEn = ahora;
        this.ultimoError = null;
    }

    public void registrarEnvio(Instant ahora) {
        this.estado = EstadoBoleta.ENVIADA;
        this.enviadaEn = ahora;
    }

    public void registrarIntento() {
        this.intentos++;
    }

    public void marcarFallida(String motivo) {
        this.estado = EstadoBoleta.FALLIDA;
        this.ultimoError = motivo == null ? null : motivo.substring(0, Math.min(motivo.length(), LARGO_MAXIMO_ERROR));
    }

    /** Vuelve a la cola: se usa al reintentar manualmente las boletas que fallaron. */
    public void reencolar() {
        this.estado = EstadoBoleta.PENDIENTE;
        this.ultimoError = null;
    }

    public boolean tienePdf() {
        return archivoClave != null && (estado == EstadoBoleta.GENERADA || estado == EstadoBoleta.ENVIADA);
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

    public String getPeriodo() {
        return periodo;
    }

    public String getTipo() {
        return tipo;
    }

    public EstadoBoleta getEstado() {
        return estado;
    }

    public String getArchivoClave() {
        return archivoClave;
    }

    public String getHash() {
        return hash;
    }

    public Integer getTamanoBytes() {
        return tamanoBytes;
    }

    public int getIntentos() {
        return intentos;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public Instant getGeneradaEn() {
        return generadaEn;
    }

    public Instant getEnviadaEn() {
        return enviadaEn;
    }
}
