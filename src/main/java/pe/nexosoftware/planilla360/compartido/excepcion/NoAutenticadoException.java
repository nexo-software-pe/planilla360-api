package pe.nexosoftware.planilla360.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class NoAutenticadoException extends PlanillaException {

    public NoAutenticadoException() {
        this("Debes iniciar sesión para continuar.");
    }

    public NoAutenticadoException(String detalle) {
        super(HttpStatus.UNAUTHORIZED, "No autenticado", detalle);
    }
}
