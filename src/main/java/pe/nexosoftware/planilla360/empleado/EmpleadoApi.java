package pe.nexosoftware.planilla360.empleado;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** API pública del módulo de empleados. Todas las consultas se limitan a la empresa del contexto. */
public interface EmpleadoApi {

    Optional<EmpleadoResumen> buscar(UUID id);

    /** Empleado vinculado a una cuenta de usuario (el portal del empleado). */
    Optional<EmpleadoResumen> deUsuario(UUID usuarioId);

    /** Empleados vigentes (ingresados y no cesados) en la fecha indicada. */
    List<EmpleadoResumen> vigentesEn(LocalDate fecha);

    /** Empleados que estuvieron vigentes en algún día del rango (ingresaron antes de su fin y no cesaron antes). */
    List<EmpleadoResumen> vigentesEntre(LocalDate desde, LocalDate hasta);

    /** Contrato que rige en la fecha indicada, si el empleado tenía alguno. */
    Optional<ContratoResumen> contratoVigenteEn(UUID empleadoId, LocalDate fecha);

    /** Hijos registrados del empleado. */
    List<HijoResumen> hijosDe(UUID empleadoId);

    /** Datos de identificación (DNI, correo, área y cargo con su nombre) de un empleado de la empresa. */
    Optional<FichaEmpleado> fichaDe(UUID empleadoId);

    /** Da de alta al empleado con su primer contrato, en una sola transacción. Devuelve su identificador. */
    UUID registrar(NuevoEmpleado datos);

    /** Ya hay un empleado de la empresa con ese DNI. */
    boolean existeDni(String dni);
}
