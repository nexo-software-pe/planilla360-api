package pe.nexosoftware.planilla360.identidad.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;
import pe.nexosoftware.planilla360.compartido.seguridad.Rol;

/**
 * Usuario del sistema. {@code tenantId} es nulo solo para el administrador de la plataforma, por eso
 * esta entidad no usa el filtro automático por empresa: el aislamiento se aplica en el servicio.
 */
@Entity
@Table(name = "usuario")
public class Usuario extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    @Column(name = "empleado_id")
    private UUID empleadoId;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "ultimo_acceso")
    private Instant ultimoAcceso;

    protected Usuario() {
    }

    public Usuario(UUID tenantId, String email, String nombre, String passwordHash, Rol rol, UUID empleadoId) {
        this.tenantId = tenantId;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.nombre = nombre.trim();
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.empleadoId = empleadoId;
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    public void registrarFallo(Instant ahora, int maximoIntentos, Duration bloqueo) {
        intentosFallidos++;
        if (intentosFallidos >= maximoIntentos) {
            bloqueadoHasta = ahora.plus(bloqueo);
            intentosFallidos = 0;
        }
    }

    public void registrarAcceso(Instant ahora) {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        ultimoAcceso = ahora;
    }

    public void cambiarPassword(String nuevoHash) {
        this.passwordHash = nuevoHash;
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    public void actualizar(String nombre, Rol rol, UUID empleadoId) {
        this.nombre = nombre.trim();
        this.rol = rol;
        this.empleadoId = empleadoId;
    }

    public void cambiarEstado(boolean activo) {
        this.activo = activo;
        if (activo) {
            this.intentosFallidos = 0;
            this.bloqueadoHasta = null;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getEmail() {
        return email;
    }

    public String getNombre() {
        return nombre;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public UUID getEmpleadoId() {
        return empleadoId;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public Instant getUltimoAcceso() {
        return ultimoAcceso;
    }
}
