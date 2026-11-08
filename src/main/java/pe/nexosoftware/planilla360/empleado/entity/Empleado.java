package pe.nexosoftware.planilla360.empleado.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UuidGenerator;
import pe.nexosoftware.planilla360.compartido.dominio.Dni;
import pe.nexosoftware.planilla360.compartido.persistencia.AuditableEntity;
import pe.nexosoftware.planilla360.empleado.model.EstadoEmpleado;

@Entity
@Table(name = "empleado")
public class Empleado extends AuditableEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 8, updatable = false)
    private String dni;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(length = 160)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "fecha_cese")
    private LocalDate fechaCese;

    @Column(name = "area_id", nullable = false)
    private UUID areaId;

    @Column(name = "cargo_id", nullable = false)
    private UUID cargoId;

    @Column(name = "jefe_id")
    private UUID jefeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEmpleado estado = EstadoEmpleado.ACTIVO;

    protected Empleado() {
    }

    public Empleado(
            Dni dni,
            String nombres,
            String apellidos,
            LocalDate fechaNacimiento,
            String email,
            String telefono,
            LocalDate fechaIngreso,
            UUID areaId,
            UUID cargoId,
            UUID jefeId) {
        this.dni = dni.valor();
        actualizar(nombres, apellidos, fechaNacimiento, email, telefono, fechaIngreso, areaId, cargoId, jefeId);
    }

    public void actualizar(
            String nombres,
            String apellidos,
            LocalDate fechaNacimiento,
            String email,
            String telefono,
            LocalDate fechaIngreso,
            UUID areaId,
            UUID cargoId,
            UUID jefeId) {
        this.nombres = nombres.trim();
        this.apellidos = apellidos.trim();
        this.fechaNacimiento = fechaNacimiento;
        this.email = email == null || email.isBlank() ? null : email.trim();
        this.telefono = telefono == null || telefono.isBlank() ? null : telefono.trim();
        this.fechaIngreso = fechaIngreso;
        this.areaId = areaId;
        this.cargoId = cargoId;
        this.jefeId = jefeId;
    }

    public void cesar(LocalDate fecha) {
        this.fechaCese = fecha;
        this.estado = EstadoEmpleado.CESADO;
    }

    public boolean estaCesado() {
        return estado == EstadoEmpleado.CESADO;
    }

    public String nombreCompleto() {
        return apellidos + ", " + nombres;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getDni() {
        return dni;
    }

    public String getNombres() {
        return nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    public LocalDate getFechaCese() {
        return fechaCese;
    }

    public UUID getAreaId() {
        return areaId;
    }

    public UUID getCargoId() {
        return cargoId;
    }

    public UUID getJefeId() {
        return jefeId;
    }

    public EstadoEmpleado getEstado() {
        return estado;
    }
}
