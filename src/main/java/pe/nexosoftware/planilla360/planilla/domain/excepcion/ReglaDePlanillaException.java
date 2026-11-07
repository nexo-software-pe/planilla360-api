package pe.nexosoftware.planilla360.planilla.domain.excepcion;

/**
 * Se incumple una regla de negocio del dominio de planilla. Java puro: la capa web la traduce a
 * {@code application/problem+json}.
 */
public class ReglaDePlanillaException extends RuntimeException {

    public ReglaDePlanillaException(String mensaje) {
        super(mensaje);
    }
}
