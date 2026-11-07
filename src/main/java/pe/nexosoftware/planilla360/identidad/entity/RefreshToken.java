package pe.nexosoftware.planilla360.identidad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/** Token de refresco opaco; solo se guarda su hash SHA-256, nunca el valor entregado al cliente. */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(nullable = false)
    private boolean revocado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected RefreshToken() {
    }

    public RefreshToken(UUID usuarioId, String tokenHash, Instant expiraEn, Instant creadoEn) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
        this.creadoEn = creadoEn;
    }

    public boolean estaVigente(Instant ahora) {
        return !revocado && expiraEn.isAfter(ahora);
    }

    public void revocar() {
        this.revocado = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public boolean isRevocado() {
        return revocado;
    }
}
