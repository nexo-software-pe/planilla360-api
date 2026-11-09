package pe.nexosoftware.planilla360.notificacion.mensajeria;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.mensajeria.Colas;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
class RabbitColaCorreos implements ColaCorreos {

    private final RabbitTemplate plantilla;

    RabbitColaCorreos(RabbitTemplate plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void encolar(CorreoBoleta correo) {
        plantilla.convertAndSend(Colas.EXCHANGE, Colas.CORREOS, correo);
    }
}
