package pe.nexosoftware.planilla360.compartido.mensajeria;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología de RabbitMQ: un exchange de trabajo y otro de mensajes muertos. Tras agotar los reintentos del
 * consumidor, el mensaje se republica en la DLQ (con el motivo en la cabecera {@code x-exception-message}).
 * Solo existe con {@code app.messaging.enabled=true}.
 */
@Configuration
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true")
class MensajeriaConfig {

    /** Los mensajes viajan como JSON (los records de cada módulo), no como objetos serializados de Java. */
    @Bean
    MessageConverter conversorJson() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    DirectExchange exchangeTrabajos() {
        return new DirectExchange(Colas.EXCHANGE, true, false);
    }

    @Bean
    DirectExchange exchangeMuertos() {
        return new DirectExchange(Colas.EXCHANGE_DLX, true, false);
    }

    @Bean
    Queue colaBoletas() {
        return conDlq(Colas.BOLETAS);
    }

    @Bean
    Queue colaBoletasDlq() {
        return QueueBuilder.durable(Colas.BOLETAS_DLQ).build();
    }

    @Bean
    Queue colaCorreos() {
        return conDlq(Colas.CORREOS);
    }

    @Bean
    Queue colaCorreosDlq() {
        return QueueBuilder.durable(Colas.CORREOS_DLQ).build();
    }

    @Bean
    Binding enlaceBoletas(Queue colaBoletas, DirectExchange exchangeTrabajos) {
        return BindingBuilder.bind(colaBoletas).to(exchangeTrabajos).with(Colas.BOLETAS);
    }

    @Bean
    Binding enlaceBoletasDlq(Queue colaBoletasDlq, DirectExchange exchangeMuertos) {
        return BindingBuilder.bind(colaBoletasDlq).to(exchangeMuertos).with(Colas.BOLETAS);
    }

    @Bean
    Binding enlaceCorreos(Queue colaCorreos, DirectExchange exchangeTrabajos) {
        return BindingBuilder.bind(colaCorreos).to(exchangeTrabajos).with(Colas.CORREOS);
    }

    @Bean
    Binding enlaceCorreosDlq(Queue colaCorreosDlq, DirectExchange exchangeMuertos) {
        return BindingBuilder.bind(colaCorreosDlq).to(exchangeMuertos).with(Colas.CORREOS);
    }

    /** Republica en la DLX con la misma clave de enrutamiento de origen: cada DLQ se enlaza con el nombre de su cola. */
    @Bean
    MessageRecoverer recuperadorADlq(RabbitTemplate plantilla) {
        RepublishMessageRecoverer recuperador = new RepublishMessageRecoverer(plantilla, Colas.EXCHANGE_DLX);
        recuperador.setErrorRoutingKeyPrefix("");
        return recuperador;
    }

    private static Queue conDlq(String nombre) {
        return QueueBuilder.durable(nombre)
                .deadLetterExchange(Colas.EXCHANGE_DLX)
                .deadLetterRoutingKey(nombre)
                .build();
    }
}
