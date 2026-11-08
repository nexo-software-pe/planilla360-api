package pe.nexosoftware.planilla360.asistencia.infrastructure.event;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.asistencia.application.port.in.JustificarDiasUseCase;
import pe.nexosoftware.planilla360.asistencia.application.port.in.JustificarDiasUseCase.Motivo;
import pe.nexosoftware.planilla360.vacaciones.PermisoAprobado;
import pe.nexosoftware.planilla360.vacaciones.VacacionesAprobadas;

/**
 * Reacciona a los eventos del módulo de vacaciones: los días aprobados dejan de contar como falta. Corre
 * en la misma transacción de la aprobación, así que o se guarda todo o no se guarda nada.
 */
@Component
class AusenciasAprobadasListener {

    private final JustificarDiasUseCase justificar;

    AusenciasAprobadasListener(JustificarDiasUseCase justificar) {
        this.justificar = justificar;
    }

    @EventListener
    void alAprobarseVacaciones(VacacionesAprobadas evento) {
        justificar.justificar(evento.empleadoId(), evento.desde(), evento.hasta(), Motivo.VACACIONES, evento.solicitudId());
    }

    @EventListener
    void alAprobarsePermiso(PermisoAprobado evento) {
        justificar.justificar(evento.empleadoId(), evento.desde(), evento.hasta(), Motivo.PERMISO, evento.permisoId());
    }
}
