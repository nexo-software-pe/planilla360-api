package pe.nexosoftware.planilla360.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class DemasiadosIntentosException extends PlanillaException {

    private final long reintentarEnSegundos;

    public DemasiadosIntentosException(long reintentarEnSegundos) {
        super(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos",
                "Superaste el límite de intentos. Reintenta en " + reintentarEnSegundos + " segundos.");
        this.reintentarEnSegundos = reintentarEnSegundos;
    }

    public long reintentarEnSegundos() {
        return reintentarEnSegundos;
    }
}
