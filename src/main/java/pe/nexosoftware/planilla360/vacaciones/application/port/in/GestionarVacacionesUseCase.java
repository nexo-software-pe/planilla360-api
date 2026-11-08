package pe.nexosoftware.planilla360.vacaciones.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import pe.nexosoftware.planilla360.vacaciones.application.model.Actor;
import pe.nexosoftware.planilla360.vacaciones.domain.model.EstadoSolicitud;
import pe.nexosoftware.planilla360.vacaciones.domain.model.SolicitudVacaciones;

public interface GestionarVacacionesUseCase {

    record DetallePeriodo(
            UUID periodoId,
            LocalDate desde,
            LocalDate hasta,
            int diasGanados,
            int diasGozados,
            int diasVendidos,
            int diasReservados,
            int diasDisponibles) {
    }

    record SaldoVacaciones(int diasDisponibles, List<DetallePeriodo> periodos) {
    }

    /** Saldo del empleado de la cuenta: lo ganado menos lo gozado, vendido y reservado por solicitudes activas. */
    SaldoVacaciones miSaldo(UUID usuarioId);

    SolicitudVacaciones solicitar(Actor actor, LocalDate desde, LocalDate hasta);

    /** Aprueba el jefe directo (RRHH solo si el empleado no tiene jefe). */
    SolicitudVacaciones aprobarPorJefe(Actor actor, UUID solicitudId);

    SolicitudVacaciones aprobarPorRrhh(Actor actor, UUID solicitudId);

    SolicitudVacaciones rechazar(Actor actor, UUID solicitudId, String motivo);

    /** Cancela el dueño de la solicitud o RRHH. */
    SolicitudVacaciones cancelar(Actor actor, UUID solicitudId);

    /** RRHH ve todas; un empleado ve las propias y las de su equipo directo. */
    List<SolicitudVacaciones> listar(Actor actor, EstadoSolicitud estado, UUID empleadoId);

    /** RRHH registra la venta de días de un periodo (solo régimen general, hasta 15 días). */
    void venderDias(Actor actor, UUID periodoId, int dias);
}
