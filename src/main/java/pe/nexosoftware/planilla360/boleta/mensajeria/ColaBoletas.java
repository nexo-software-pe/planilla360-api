package pe.nexosoftware.planilla360.boleta.mensajeria;

/** Puerto de salida: encola la generación de una boleta (RabbitMQ, o en el mismo proceso si no hay mensajería). */
public interface ColaBoletas {

    void encolar(TrabajoBoleta trabajo);
}
