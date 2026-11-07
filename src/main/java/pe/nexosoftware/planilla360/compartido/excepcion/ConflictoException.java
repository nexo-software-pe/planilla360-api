package pe.nexosoftware.planilla360.compartido.excepcion;

import org.springframework.http.HttpStatus;

public class ConflictoException extends PlanillaException {

    public ConflictoException(String detalle) {
        super(HttpStatus.CONFLICT, "Conflicto con el estado actual", detalle);
    }
}
