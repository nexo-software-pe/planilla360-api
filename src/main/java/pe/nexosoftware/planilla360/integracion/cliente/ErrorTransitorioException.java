package pe.nexosoftware.planilla360.integracion.cliente;

/** Fallo del servicio externo que puede pasar solo (timeout, red, 5xx): se reintenta y cuenta para el cortacircuito. */
public class ErrorTransitorioException extends RuntimeException {

    public ErrorTransitorioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
