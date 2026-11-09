package pe.nexosoftware.planilla360.notificacion.mensajeria;

/** Puerto de salida: encola un correo (RabbitMQ, o envío en el mismo proceso si no hay mensajería). */
public interface ColaCorreos {

    void encolar(CorreoBoleta correo);
}
