package pe.nexosoftware.planilla360.compartido.mensajeria;

/** Nombres de las colas de RabbitMQ. Cada cola de trabajo tiene su cola de mensajes muertos (DLQ). */
public final class Colas {

    public static final String EXCHANGE = "planilla360.trabajos";
    public static final String EXCHANGE_DLX = "planilla360.trabajos.dlx";

    public static final String BOLETAS = "boletas.generar";
    public static final String BOLETAS_DLQ = "boletas.generar.dlq";
    public static final String CORREOS = "notificaciones.correo";
    public static final String CORREOS_DLQ = "notificaciones.correo.dlq";

    private Colas() {
    }
}
