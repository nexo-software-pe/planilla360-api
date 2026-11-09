package pe.nexosoftware.planilla360.boleta.mensajeria;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.mensajeria.Colas;

@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
class RabbitColaBoletas implements ColaBoletas {

    private final RabbitTemplate plantilla;

    RabbitColaBoletas(RabbitTemplate plantilla) {
        this.plantilla = plantilla;
    }

    @Override
    public void encolar(TrabajoBoleta trabajo) {
        plantilla.convertAndSend(Colas.EXCHANGE, Colas.BOLETAS, trabajo);
    }
}
