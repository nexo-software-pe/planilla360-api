package pe.nexosoftware.planilla360.vacaciones.infrastructure.adapter;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.vacaciones.PermisoAprobado;
import pe.nexosoftware.planilla360.vacaciones.VacacionesAprobadas;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.PublicadorEventosPort;

/** Publica los eventos con el mecanismo de eventos de Spring (síncrono, en la misma transacción). */
@Component
class EventosSpringAdapter implements PublicadorEventosPort {

    private final ApplicationEventPublisher publicador;

    EventosSpringAdapter(ApplicationEventPublisher publicador) {
        this.publicador = publicador;
    }

    @Override
    public void publicar(VacacionesAprobadas evento) {
        publicador.publishEvent(evento);
    }

    @Override
    public void publicar(PermisoAprobado evento) {
        publicador.publishEvent(evento);
    }
}
