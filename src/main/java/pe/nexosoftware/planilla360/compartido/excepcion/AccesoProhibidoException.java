package pe.nexosoftware.planilla360.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class AccesoProhibidoException extends PlanillaException {

    public AccesoProhibidoException(String detalle) {
        super(HttpStatus.FORBIDDEN, "Acceso prohibido", detalle);
    }
}
